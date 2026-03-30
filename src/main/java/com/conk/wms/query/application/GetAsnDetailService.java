package com.conk.wms.query.application;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.application.dto.AsnDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
// ASN 상세 조회 전용 query service.
// 현재는 seller tenant 범위 안에서만 조회하고, 화면이 바로 쓸 수 있는 상세 응답 shape로 가공한다.
public class GetAsnDetailService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;

    public GetAsnDetailService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                               WarehouseRepository warehouseRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public AsnDetailResponse getAsnDetail(String sellerId, String asnId) {
        Asn asn = asnRepository.findByAsnIdAndSellerId(asnId, sellerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
                ));

        List<AsnItem> items = asnItemRepository.findAllByAsnId(asnId);
        String warehouseName = warehouseRepository.findById(asn.getWarehouseId())
                .map(Warehouse::getName)
                .orElse(asn.getWarehouseId());

        int totalQuantity = items.stream()
                .mapToInt(AsnItem::getQuantity)
                .sum();
        int totalCartons = items.stream()
                .mapToInt(AsnItem::getBoxQuantity)
                .sum();
        String skuSummary = items.stream()
                .map(AsnItem::getSkuId)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");

        List<AsnDetailResponse.ItemResponse> itemResponses = items.stream()
                .map(item -> AsnDetailResponse.ItemResponse.builder()
                        .sku(item.getSkuId())
                        .productName(item.getProductNameSnapshot())
                        .quantity(item.getQuantity())
                        .cartons(item.getBoxQuantity())
                        .build())
                .toList();

        return AsnDetailResponse.builder()
                .id(asn.getAsnId())
                .asnNo(asn.getAsnId())
                .status(toSellerStatus(asn.getStatus()))
                .company(asn.getSellerId())
                .seller(asn.getSellerId())
                .warehouse(warehouseName)
                .warehouseName(warehouseName)
                .skuCount((int) items.stream().map(AsnItem::getSkuId).distinct().count())
                .totalQuantity(totalQuantity)
                .plannedQty(totalQuantity)
                .actualQty(null)
                .sku(skuSummary)
                .expectedDate(asn.getExpectedArrivalDate().toString())
                .createdAt(asn.getCreatedAt().toLocalDate().toString())
                .registeredDate(asn.getCreatedAt().toLocalDate().toString())
                .referenceNo(buildReferenceNo(asn.getAsnId()))
                .note(asn.getSellerMemo())
                .detail(AsnDetailResponse.DetailResponse.builder()
                        .supplierName(asn.getSellerId())
                        // 현재 ERD에 운송/서류 저장 컬럼이 없어, 상세 모달이 깨지지 않도록 안전한 기본값을 넣는다.
                        .originCountry("정보 없음")
                        .originPort("-")
                        .transportMode("-")
                        .incoterms("-")
                        .bookingNo(buildReferenceNo(asn.getAsnId()))
                        .carrier("정보 없음")
                        .arrivalWindow(asn.getExpectedArrivalDate().toString())
                        .documents(List.of("Packing List"))
                        .totalCartons(totalCartons)
                        .items(itemResponses)
                        .build())
                .build();
    }

    private String toSellerStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SUBMITTED";
        }
        return switch (status) {
            case "REGISTERED" -> "SUBMITTED";
            case "ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED" -> "RECEIVED";
            case "CANCELED", "CANCELLED" -> "CANCELLED";
            default -> status;
        };
    }

    private String buildReferenceNo(String asnId) {
        if (asnId == null || asnId.isBlank()) {
            return "REF-PENDING";
        }
        int startIndex = Math.max(0, asnId.length() - 6);
        return "REF-" + asnId.substring(startIndex);
    }
}
