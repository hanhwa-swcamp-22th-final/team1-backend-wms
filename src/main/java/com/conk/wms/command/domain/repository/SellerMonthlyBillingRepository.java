package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * seller 월 정산 결과를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface SellerMonthlyBillingRepository extends JpaRepository<SellerMonthlyBilling, Long> {

    Optional<SellerMonthlyBilling> findByBillingMonthAndSellerIdAndWarehouseId(
            String billingMonth,
            String sellerId,
            String warehouseId
    );

    List<SellerMonthlyBilling> findByBillingMonthOrderBySellerIdAscWarehouseIdAsc(String billingMonth);
}
