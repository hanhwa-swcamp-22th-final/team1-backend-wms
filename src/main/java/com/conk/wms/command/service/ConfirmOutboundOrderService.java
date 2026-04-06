package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 송장 발행까지 끝난 주문을 최종 출고 확정하고 ALLOCATED 재고를 마감하는 서비스다.
 */
@Service
public class ConfirmOutboundOrderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OutboundPendingRepository outboundPendingRepository;
    private final WorkDetailRepository workDetailRepository;
    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final InventoryRepository inventoryRepository;
    private final OutboundCompletedRepository outboundCompletedRepository;

    public ConfirmOutboundOrderService(OutboundPendingRepository outboundPendingRepository,
                                       WorkDetailRepository workDetailRepository,
                                       AllocatedInventoryRepository allocatedInventoryRepository,
                                       InventoryRepository inventoryRepository,
                                       OutboundCompletedRepository outboundCompletedRepository) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.workDetailRepository = workDetailRepository;
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.outboundCompletedRepository = outboundCompletedRepository;
    }

    /**
     * 주문 한 건을 최종 출고 확정하고 ALLOCATED 재고를 차감 마감한다.
     */
    @Transactional
    public ConfirmResult confirm(String orderId, String tenantCode, String actorId) {
        if (outboundCompletedRepository.existsByIdOrderIdAndIdTenantId(orderId, tenantCode)) {
            throw new BusinessException(ErrorCode.OUTBOUND_CONFIRM_ALREADY_COMPLETED);
        }

        List<OutboundPending> pendingRows = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        if (pendingRows.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_CONFIRM_SOURCE_NOT_FOUND,
                    ErrorCode.OUTBOUND_CONFIRM_SOURCE_NOT_FOUND.getMessage() + ": " + orderId
            );
        }

        validateReady(orderId, pendingRows);

        List<AllocatedInventory> allocatedRows = allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        if (allocatedRows.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_CONFIRM_SOURCE_NOT_FOUND,
                    ErrorCode.OUTBOUND_CONFIRM_SOURCE_NOT_FOUND.getMessage() + ": " + orderId
            );
        }

        LocalDateTime confirmedAt = LocalDateTime.now();
        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        int releasedRowCount = 0;

        for (AllocatedInventory allocated : allocatedRows) {
            Inventory allocatedInventory = inventoryRepository
                    .findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(
                            allocated.getId().getLocationId(),
                            allocated.getId().getSkuId(),
                            tenantCode,
                            "ALLOCATED"
                    )
                    .orElseThrow(() -> new BusinessException(ErrorCode.OUTBOUND_CONFIRM_ALLOCATED_NOT_FOUND));

            allocatedInventory.deduct(allocated.getQuantity());
            inventoryRepository.save(allocatedInventory);

            allocated.release(actor, confirmedAt);
            allocatedInventoryRepository.save(allocated);
            releasedRowCount++;
        }

        outboundCompletedRepository.save(new OutboundCompleted(orderId, tenantCode, actor, confirmedAt));

        return new ConfirmResult(orderId, "CONFIRMED", releasedRowCount, confirmedAt);
    }

    /**
     * 여러 주문을 순차적으로 출고 확정한다. 현재는 fail-fast 방식으로 단순화했다.
     */
    @Transactional
    public BulkConfirmResult confirmBulk(List<String> orderIds, String tenantCode, String actorId, boolean includeCsv) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.OUTBOUND_CONFIRM_ORDER_IDS_REQUIRED);
        }

        int releasedRowCount = 0;
        for (String orderId : orderIds) {
            releasedRowCount += confirm(orderId, tenantCode, actorId).getReleasedRowCount();
        }

        return new BulkConfirmResult(orderIds.size(), releasedRowCount, includeCsv);
    }

    private void validateReady(String orderId, List<OutboundPending> pendingRows) {
        boolean invoiceIssued = pendingRows.stream().allMatch(pending -> pending.getInvoiceIssuedAt() != null);
        List<WorkDetail> details = workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(orderId);
        boolean packed = !details.isEmpty() && details.stream().allMatch(detail -> detail.getCompletedAt() != null);
        if (!invoiceIssued || !packed) {
            throw new BusinessException(ErrorCode.OUTBOUND_CONFIRM_NOT_READY);
        }
    }

    public static class ConfirmResult {
        private final String orderId;
        private final String status;
        private final int releasedRowCount;
        private final LocalDateTime confirmedAt;

        public ConfirmResult(String orderId, String status, int releasedRowCount, LocalDateTime confirmedAt) {
            this.orderId = orderId;
            this.status = status;
            this.releasedRowCount = releasedRowCount;
            this.confirmedAt = confirmedAt;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getStatus() {
            return status;
        }

        public int getReleasedRowCount() {
            return releasedRowCount;
        }

        public LocalDateTime getConfirmedAt() {
            return confirmedAt;
        }

        public String getFormattedConfirmedAt() {
            return confirmedAt == null ? null : confirmedAt.format(DATE_FORMATTER);
        }
    }

    public static class BulkConfirmResult {
        private final int confirmedOrderCount;
        private final int releasedRowCount;
        private final boolean includeCsv;

        public BulkConfirmResult(int confirmedOrderCount, int releasedRowCount, boolean includeCsv) {
            this.confirmedOrderCount = confirmedOrderCount;
            this.releasedRowCount = releasedRowCount;
            this.includeCsv = includeCsv;
        }

        public int getConfirmedOrderCount() {
            return confirmedOrderCount;
        }

        public int getReleasedRowCount() {
            return releasedRowCount;
        }

        public boolean isIncludeCsv() {
            return includeCsv;
        }
    }
}
