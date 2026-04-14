package com.conk.wms.command.application.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.WarehouseManagerAssignment;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseManagerAssignmentRepository;
import com.conk.wms.command.infrastructure.kafka.event.AsnCreatedEvent;
import com.conk.wms.command.infrastructure.kafka.publisher.NotificationEventKafkaPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 셀러 ASN 등록 요청을 실제 ASN/ASN_ITEM 엔티티로 생성하는 서비스다.
 */
@Service
// Seller ASN 등록 유스케이스의 핵심 흐름:
// 1) 입력 검증 -> 2) 창고/중복 검사 -> 3) ASN 헤더 저장 -> 4) ASN 품목 저장
public class RegisterAsnService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final SellerWarehouseRepository sellerWarehouseRepository;
    private final WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository;
    private final NotificationEventKafkaPublisher notificationEventKafkaPublisher;

    public RegisterAsnService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                              WarehouseRepository warehouseRepository,
                              SellerWarehouseRepository sellerWarehouseRepository,
                              WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository,
                              NotificationEventKafkaPublisher notificationEventKafkaPublisher) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.sellerWarehouseRepository = sellerWarehouseRepository;
        this.warehouseManagerAssignmentRepository = warehouseManagerAssignmentRepository;
        this.notificationEventKafkaPublisher = notificationEventKafkaPublisher;
    }

    /**
     * 셀러가 보낸 ASN 등록 정보를 헤더와 품목으로 분리 저장한다.
     * 저장 전에 창고 존재 여부, ASN 중복 여부, SKU 중복 여부를 한 번에 점검한다.
     */
    @Transactional
    public void register(RegisterAsnCommand command) {
        validateCommand(command);

        if (!warehouseRepository.existsById(command.getWarehouseId())) {
            throw new BusinessException(
                    ErrorCode.ASN_WAREHOUSE_NOT_FOUND,
                    buildDetailedMessage(ErrorCode.ASN_WAREHOUSE_NOT_FOUND, command.getWarehouseId())
            );
        }
        assertSellerUsesWarehouse(command.getSellerId(), command.getWarehouseId());
        if (asnRepository.existsByAsnId(command.getAsnId())) {
            throw new BusinessException(
                    ErrorCode.ASN_ALREADY_EXISTS,
                    buildDetailedMessage(ErrorCode.ASN_ALREADY_EXISTS, command.getAsnId())
            );
        }

        Set<String> seen = new HashSet<>();
        command.getItems().forEach(item -> {
            if (!seen.add(item.getSkuId())) {
                throw new BusinessException(
                        ErrorCode.ASN_DUPLICATE_SKU,
                        buildDetailedMessage(ErrorCode.ASN_DUPLICATE_SKU, item.getSkuId())
                );
            }
        });

        LocalDateTime now = LocalDateTime.now();
        // 헤더 박스 수는 item 입력을 신뢰하되, 최종 값은 서버에서 합산해서 저장한다.
        int totalBoxQuantity = command.getItems().stream()
                .mapToInt(RegisterAsnItemCommand::getBoxQuantity)
                .sum();

        // ERD 운영 상태는 현재 REGISTERED로 저장하고,
        // seller 조회 화면에는 이후 query layer에서 SUBMITTED로 변환해서 보여준다.
        asnRepository.save(new Asn(
                command.getAsnId(),
                command.getWarehouseId(),
                command.getSellerId(),
                command.getExpectedDate(),
                "REGISTERED",
                command.getSellerMemo(),
                totalBoxQuantity,
                now,
                now,
                command.getSellerId(),
                command.getSellerId()
        ));

        command.getItems().forEach(item ->
                asnItemRepository.save(new AsnItem(
                        command.getAsnId(),
                        item.getSkuId(),
                        item.getQuantity(),
                        item.getProductNameSnapshot(),
                        item.getBoxQuantity()
                ))
        );
        warehouseRepository.findById(command.getWarehouseId())
                .map(warehouse -> warehouseManagerAssignmentRepository.findByWarehouseIdAndTenantId(
                        command.getWarehouseId(),
                        warehouse.getTenantId()
                ))
                .flatMap(java.util.function.Function.identity())
                .map(WarehouseManagerAssignment::getManagerAccountId)
                .filter(this::isBlankNegated)
                .ifPresent(managerId -> {
                    AsnCreatedEvent event = new AsnCreatedEvent();
                    event.setAsnId(command.getAsnId());
                    event.setManagerId(managerId);
                    event.setAsnCount(1);
                    event.setExpectedDate(command.getExpectedDate().toString());
                    event.setTimestamp(LocalDateTime.now());
                    notificationEventKafkaPublisher.publishAsnCreated(event);
                });
    }

    // 서비스 레벨 사전 검증.
    // 도메인 저장 전에 걸러야 하는 형식/필수값 오류는 여기서 공통 에러 코드로 정리한다.
    private void validateCommand(RegisterAsnCommand command) {
        if (isBlank(command.getAsnId())) {
            throw new BusinessException(ErrorCode.ASN_ID_REQUIRED);
        }
        if (isBlank(command.getWarehouseId())) {
            throw new BusinessException(ErrorCode.ASN_WAREHOUSE_ID_REQUIRED);
        }
        if (isBlank(command.getSellerId())) {
            throw new BusinessException(ErrorCode.ASN_SELLER_ID_REQUIRED);
        }
        if (command.getExpectedDate() == null) {
            throw new BusinessException(ErrorCode.ASN_EXPECTED_DATE_REQUIRED);
        }
        if (command.getSellerMemo() != null && command.getSellerMemo().length() > 500) {
            throw new BusinessException(ErrorCode.ASN_SELLER_MEMO_TOO_LONG);
        }

        List<RegisterAsnItemCommand> items = command.getItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.ASN_ITEMS_REQUIRED);
        }

        items.forEach(item -> {
            if (isBlank(item.getSkuId())) {
                throw new BusinessException(ErrorCode.ASN_SKU_REQUIRED);
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.ASN_INVALID_QUANTITY);
            }
            if (item.getBoxQuantity() <= 0) {
                throw new BusinessException(ErrorCode.ASN_INVALID_BOX_QUANTITY);
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isBlankNegated(String value) {
        return !isBlank(value);
    }

    private void assertSellerUsesWarehouse(String sellerId, String warehouseId) {
        if (!sellerWarehouseRepository.existsByIdSellerIdAndIdWarehouseId(sellerId, warehouseId)) {
            throw new BusinessException(
                    ErrorCode.ASN_SELLER_WAREHOUSE_MISMATCH,
                    ErrorCode.ASN_SELLER_WAREHOUSE_MISMATCH.getMessage() + ": " + sellerId + " -> " + warehouseId
            );
        }
    }

    // ErrorCode의 기본 문구를 유지하면서도 실제 문제 값을 같이 보여주기 위한 도우미.
    private String buildDetailedMessage(ErrorCode errorCode, String value) {
        String baseMessage = errorCode.getMessage();
        if (baseMessage.endsWith(".")) {
            baseMessage = baseMessage.substring(0, baseMessage.length() - 1);
        }
        return baseMessage + ": " + value;
    }
}


