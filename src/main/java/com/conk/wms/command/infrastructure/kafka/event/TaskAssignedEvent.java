package com.conk.wms.command.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * 작업 배정 Kafka 이벤트 DTO
 *
 * wms-service가 "wms.task.assigned" 토픽에 발행하는 메시지의 JSON 구조를 정의한다.
 * Kafka로 보내기 전에 ObjectMapper가 이 클래스를 JSON 문자열로 직렬화한다.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true):
 *   현재는 발행 측 DTO이지만, 추후 consumer나 테스트에서 이 클래스를 재사용할 때
 *   예상하지 못한 필드가 들어와도 호환성을 유지하기 쉽도록 항상 붙여둔다.
 *
 * 발행 측(wms-service)이 전송해야 하는 JSON 예시:
 * {
 *   "workerId": "1001",
 *   "roleId": "ROLE_WH_WORKER",
 *   "assignedCount": 3,
 *   "tenantId": "tenant-001",
 *   "timestamp": "2026-04-05T10:00:00"
 * }
 *
 * 비즈니스 로직 연결 가이드:
 *   추천 연결 지점은 AssignTaskService.assign(...)의 DB 저장이 모두 끝난 직후다.
 *   work_assignment / work_detail 저장이 성공한 뒤 아래 데이터를 모아 발행한다.
 *   - workerId: assign(...)에 전달된 작업자 accountId
 *   - roleId: 현재 계약상 "ROLE_WH_WORKER"
 *   - assignedCount: 이번 배정으로 생성된 작업 상세 건수 또는 배정 결과 건수
 *   - tenantId: assign(...)에 전달된 tenantCode
 *   - timestamp: WMS 서버 기준 LocalDateTime.now()
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskAssignedEvent {

    /**
     * 작업을 배정받은 창고 작업자(WH_WORKER)의 accountId
     * member-service Account.accountId를 문자열로 전달한다.
     */
    private String workerId;

    /**
     * 수신자의 역할 ID
     * notification 테이블의 role_id 컬럼에 저장된다.
     */
    private String roleId;

    /** 배정된 작업 건수 */
    private int assignedCount;

    /** 테넌트 ID (데이터 격리용, 로깅 및 추적에 활용) */
    private String tenantId;

    /** 이벤트 발생 시각 (wms-service에서 세팅) */
    private LocalDateTime timestamp;

    // Jackson이 직렬화/역직렬화 보조 처리할 때 기본 생성자가 있으면 테스트와 확장에 유리하다.
    public TaskAssignedEvent() {
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getRoleId() {
        return roleId;
    }

    public int getAssignedCount() {
        return assignedCount;
    }

    public String getTenantId() {
        return tenantId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
