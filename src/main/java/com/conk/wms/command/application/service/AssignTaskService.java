package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.command.infrastructure.kafka.event.TaskAssignedEvent;
import com.conk.wms.command.infrastructure.kafka.publisher.NotificationEventKafkaPublisher;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 출고 지시된 주문을 작업자에게 배정하고 work_detail을 생성하는 서비스다.
 */
@Service
public class AssignTaskService {

    private static final String WORK_ID_PREFIX = "WORK-OUT-";

    private final OutboundPendingRepository outboundPendingRepository;
    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final AutoAssignTaskService autoAssignTaskService;
    private final NotificationEventKafkaPublisher notificationEventKafkaPublisher;

    public AssignTaskService(OutboundPendingRepository outboundPendingRepository,
                             AllocatedInventoryRepository allocatedInventoryRepository,
                             WorkAssignmentRepository workAssignmentRepository,
                             WorkDetailRepository workDetailRepository,
                             AutoAssignTaskService autoAssignTaskService,
                             NotificationEventKafkaPublisher notificationEventKafkaPublisher) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.autoAssignTaskService = autoAssignTaskService;
        this.notificationEventKafkaPublisher = notificationEventKafkaPublisher;
    }

    /**
     * 출고 지시가 끝난 주문을 작업자에게 배정하고 작업 헤더/상세를 함께 만든다.
     * 이미 배정된 주문이면 기존 row를 지우고 같은 주문 기준으로 재배정한다.
     */
    @Transactional
    public AssignResult assign(String orderId, String tenantCode, String workerId, String assignedByAccountId) {
        if (workerId == null || workerId.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORKER_REQUIRED);
        }
        if (!outboundPendingRepository.existsByIdOrderIdAndIdTenantId(orderId, tenantCode)) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_ASSIGNMENT_SOURCE_NOT_FOUND,
                    ErrorCode.OUTBOUND_ASSIGNMENT_SOURCE_NOT_FOUND.getMessage() + ": " + orderId
            );
        }

        String workId = buildWorkId(orderId, tenantCode);
        boolean reassigned = !workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(orderId, tenantCode).isEmpty();
        if (reassigned) {
            autoAssignTaskService.clearExistingAssignments(orderId, tenantCode);
        }

        String actor = assignedByAccountId == null || assignedByAccountId.isBlank() ? "SYSTEM" : assignedByAccountId;
        workAssignmentRepository.save(new WorkAssignment(workId, tenantCode, workerId, actor));
        int assignedCount = allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode).stream()
                .mapToInt(allocated -> {
                workDetailRepository.save(new WorkDetail(
                        workId,
                        orderId,
                        allocated.getId().getSkuId(),
                        allocated.getId().getLocationId(),
                        allocated.getQuantity(),
                        actor
                ));
                return 1;
            })
            .sum();

        TaskAssignedEvent event = new TaskAssignedEvent();
        event.setWorkerId(workerId);
        event.setRoleId("ROLE_WH_WORKER");
        event.setAssignedCount(assignedCount);
        event.setTenantId(tenantCode);
        event.setTimestamp(LocalDateTime.now());
        notificationEventKafkaPublisher.publishTaskAssigned(event);

        return new AssignResult(workId, orderId, workerId, reassigned);
    }

    private String buildWorkId(String orderId, String tenantCode) {
        return WORK_ID_PREFIX + sanitizeForId(tenantCode) + "-" + orderId;
    }

    private String sanitizeForId(String value) {
        return value == null ? "UNKNOWN" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public static class AssignResult {
        private final String workId;
        private final String orderId;
        private final String workerId;
        private final boolean reassigned;

        public AssignResult(String workId, String orderId, String workerId, boolean reassigned) {
            this.workId = workId;
            this.orderId = orderId;
            this.workerId = workerId;
            this.reassigned = reassigned;
        }

        public String getWorkId() {
            return workId;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getWorkerId() {
            return workerId;
        }

        public boolean isReassigned() {
            return reassigned;
        }
    }
}

