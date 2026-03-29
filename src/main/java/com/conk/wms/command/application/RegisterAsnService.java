package com.conk.wms.command.application;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RegisterAsnService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;

    public RegisterAsnService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                              WarehouseRepository warehouseRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public void register(RegisterAsnCommand command) {
        validateCommand(command);

        if (!warehouseRepository.existsById(command.getWarehouseId())) {
            throw new BusinessException(
                    ErrorCode.ASN_WAREHOUSE_NOT_FOUND,
                    buildDetailedMessage(ErrorCode.ASN_WAREHOUSE_NOT_FOUND, command.getWarehouseId())
            );
        }
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
        int totalBoxQuantity = command.getItems().stream()
                .mapToInt(RegisterAsnItemCommand::getBoxQuantity)
                .sum();

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
    }

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

    private String buildDetailedMessage(ErrorCode errorCode, String value) {
        String baseMessage = errorCode.getMessage();
        if (baseMessage.endsWith(".")) {
            baseMessage = baseMessage.substring(0, baseMessage.length() - 1);
        }
        return baseMessage + ": " + value;
    }
}
