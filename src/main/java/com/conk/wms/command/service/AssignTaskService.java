package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignTaskService {

    private static final String WORK_ID_PREFIX = "WORK-OUT-";

    private final OutboundPendingRepository outboundPendingRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    public AssignTaskService(OutboundPendingRepository outboundPendingRepository,
                             WorkAssignmentRepository workAssignmentRepository) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.workAssignmentRepository = workAssignmentRepository;
    }

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

        String workId = buildWorkId(orderId);
        boolean reassigned = !workAssignmentRepository.findAllByIdWorkIdAndIdTenantId(workId, tenantCode).isEmpty();
        if (reassigned) {
            workAssignmentRepository.deleteAllByIdWorkIdAndIdTenantId(workId, tenantCode);
        }

        String actor = assignedByAccountId == null || assignedByAccountId.isBlank() ? "SYSTEM" : assignedByAccountId;
        workAssignmentRepository.save(new WorkAssignment(workId, tenantCode, workerId, actor));

        return new AssignResult(workId, orderId, workerId, reassigned);
    }

    private String buildWorkId(String orderId) {
        return WORK_ID_PREFIX + orderId;
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
