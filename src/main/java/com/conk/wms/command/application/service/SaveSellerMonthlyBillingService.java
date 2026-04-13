package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import com.conk.wms.command.domain.repository.SellerMonthlyBillingRepository;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * batch-service가 보낸 월 정산 결과를 WMS 저장소에 upsert한다.
 */
@Service
public class SaveSellerMonthlyBillingService {

    private final SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    public SaveSellerMonthlyBillingService(SellerMonthlyBillingRepository sellerMonthlyBillingRepository) {
        this.sellerMonthlyBillingRepository = sellerMonthlyBillingRepository;
    }

    @Transactional
    public SellerMonthlyBilling save(BillingMonthlyResultEvent event) {
        validate(event);

        return sellerMonthlyBillingRepository.findByBillingMonthAndSellerIdAndWarehouseId(
                        event.getBillingMonth(),
                        event.getSellerId(),
                        event.getWarehouseId()
                )
                .map(existing -> {
                    existing.updateFrom(event);
                    return existing;
                })
                .orElseGet(() -> sellerMonthlyBillingRepository.save(SellerMonthlyBilling.from(event)));
    }

    private void validate(BillingMonthlyResultEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        requireText(event.getBillingMonth(), "billingMonth must not be blank");
        requireText(event.getSellerId(), "sellerId must not be blank");
        requireText(event.getWarehouseId(), "warehouseId must not be blank");
        Objects.requireNonNull(event.getCalculatedAt(), "calculatedAt must not be null");
        Objects.requireNonNull(event.getVersion(), "version must not be null");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
