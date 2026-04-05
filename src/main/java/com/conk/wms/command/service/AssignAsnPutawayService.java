package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.dto.AssignAsnPutawayCommand;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.support.PutawayLocationSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ASN별 BIN 배정 결과를 저장하는 command 서비스다.
 */
@Service
@Transactional
public class AssignAsnPutawayService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final LocationRepository locationRepository;
    private final PutawayLocationSupport putawayLocationSupport;

    public AssignAsnPutawayService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                   InspectionPutawayRepository inspectionPutawayRepository,
                                   LocationRepository locationRepository,
                                   PutawayLocationSupport putawayLocationSupport) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.locationRepository = locationRepository;
        this.putawayLocationSupport = putawayLocationSupport;
    }

    /**
     * ASN 품목별로 최종 선택된 BIN(locationId)을 inspection_putaway에 선저장한다.
     * 이후 inspection 저장 단계는 이 locationId를 그대로 재사용한다.
     */
    public int assign(AssignAsnPutawayCommand command) {
        validateItems(command.getItems());

        Asn asn = asnRepository.findByAsnId(command.getAsnId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + command.getAsnId()
                ));
        validateAsnStatus(asn.getStatus());

        Map<String, AsnItem> asnItemBySkuId = asnItemRepository.findAllByAsnId(command.getAsnId()).stream()
                .collect(Collectors.toMap(AsnItem::getSkuId, Function.identity()));
        PutawayLocationSupport.AssignmentContext context =
                putawayLocationSupport.buildContext(asn.getWarehouseId(), command.getTenantCode());

        Set<String> seenSkuIds = new HashSet<>();
        Map<String, String> requestedSkuByLocationId = new HashMap<>();
        for (AssignAsnPutawayCommand.ItemCommand item : command.getItems()) {
            validateItem(item, asn, asnItemBySkuId, context, seenSkuIds, requestedSkuByLocationId);
        }

        for (AssignAsnPutawayCommand.ItemCommand item : command.getItems()) {
            // Bin 미배정 화면의 최종 결과는 별도 테이블이 아니라 inspection_putaway.locationId에 선저장한다.
            // 이후 inspection 저장에서는 이 locationId를 그대로 재사용한다.
            InspectionPutaway row = inspectionPutawayRepository.findByAsnIdAndSkuId(command.getAsnId(), item.getSkuId())
                    .orElseGet(() -> new InspectionPutaway(command.getAsnId(), item.getSkuId(), command.getTenantCode()));
            row.assignLocation(item.getLocationId());
            inspectionPutawayRepository.save(row);
        }

        return command.getItems().size();
    }

    private void validateItems(List<AssignAsnPutawayCommand.ItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.ASN_PUTAWAY_ITEMS_REQUIRED);
        }
    }

    private void validateAsnStatus(String status) {
        // 현재 프론트 연결 단계에서는 Bin 배정만 먼저 저장할 수 있게 폭넓게 허용한다.
        if (!"REGISTERED".equals(status) && !"ARRIVED".equals(status) && !"INSPECTING_PUTAWAY".equals(status)) {
            throw new BusinessException(
                    ErrorCode.ASN_PUTAWAY_ASSIGN_NOT_ALLOWED,
                    ErrorCode.ASN_PUTAWAY_ASSIGN_NOT_ALLOWED.getMessage() + ": " + status
            );
        }
    }

    private void validateItem(AssignAsnPutawayCommand.ItemCommand item, Asn asn, Map<String, AsnItem> asnItemBySkuId,
                              PutawayLocationSupport.AssignmentContext context, Set<String> seenSkuIds,
                              Map<String, String> requestedSkuByLocationId) {
        if (item.getSkuId() == null || item.getSkuId().isBlank() || !asnItemBySkuId.containsKey(item.getSkuId())) {
            throw new BusinessException(
                    ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND,
                    ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND.getMessage() + ": " + item.getSkuId()
            );
        }
        if (!seenSkuIds.add(item.getSkuId())) {
            throw new BusinessException(
                    ErrorCode.ASN_DUPLICATE_SKU,
                    ErrorCode.ASN_DUPLICATE_SKU.getMessage() + ": " + item.getSkuId()
            );
        }
        if (item.getLocationId() == null || item.getLocationId().isBlank()) {
            throw new BusinessException(ErrorCode.ASN_PUTAWAY_LOCATION_REQUIRED);
        }

        Location location = locationRepository.findById(item.getLocationId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_LOCATION_NOT_FOUND,
                        ErrorCode.ASN_LOCATION_NOT_FOUND.getMessage() + ": " + item.getLocationId()
                ));
        if (!asn.getWarehouseId().equals(location.getWarehouseId())) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_WAREHOUSE_MISMATCH,
                    ErrorCode.ASN_LOCATION_WAREHOUSE_MISMATCH.getMessage() + ": " + item.getLocationId()
                );
        }
        if (!location.isActive()) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_INACTIVE,
                    ErrorCode.ASN_LOCATION_INACTIVE.getMessage() + ": " + item.getLocationId()
            );
        }

        PutawayLocationSupport.LocationSnapshot snapshot = context.getSnapshotByLocationId().get(item.getLocationId());
        if (snapshot == null) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_INACTIVE,
                    ErrorCode.ASN_LOCATION_INACTIVE.getMessage() + ": " + item.getLocationId()
            );
        }

        String alreadyRequestedSku = requestedSkuByLocationId.putIfAbsent(item.getLocationId(), item.getSkuId());
        if (alreadyRequestedSku != null && !alreadyRequestedSku.equals(item.getSkuId())) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED,
                    ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED.getMessage() + ": " + item.getLocationId()
            );
        }

        if (snapshot.isOccupiedByDifferentSku(item.getSkuId())) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED,
                    ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED.getMessage() + ": " + item.getLocationId()
            );
        }

        List<InspectionPutaway> openRows = inspectionPutawayRepository.findAllByLocationIdAndCompletedFalse(item.getLocationId());
        Optional<InspectionPutaway> occupiedByOtherSku = openRows.stream()
                .filter(row -> !commandOwnsRow(asn.getAsnId(), item.getSkuId(), row))
                .filter(row -> !item.getSkuId().equals(row.getSkuId()))
                .findAny();
        if (occupiedByOtherSku.isPresent()) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED,
                    ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED.getMessage() + ": " + item.getLocationId()
            );
        }

        // 수량 정보가 아직 없더라도, ASN 예정 수량 기준으로 적재 가능 capacity는 미리 확인한다.
        AsnItem asnItem = asnItemBySkuId.get(item.getSkuId());
        if (!snapshot.hasEnoughCapacity(asnItem.getQuantity())) {
            throw new BusinessException(
                    ErrorCode.ASN_LOCATION_CAPACITY_EXCEEDED,
                    ErrorCode.ASN_LOCATION_CAPACITY_EXCEEDED.getMessage() + ": " + item.getLocationId()
            );
        }
    }

    private boolean commandOwnsRow(String asnId, String skuId, InspectionPutaway row) {
        return asnId.equals(row.getAsnId()) && skuId.equals(row.getSkuId());
    }
}
