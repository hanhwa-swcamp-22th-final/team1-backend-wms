package com.conk.wms.command.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * 입고예정(ASN) 등록 Kafka 이벤트 DTO
 *
 * wms-service가 "wms.asn.created" 토픽에 발행하는 메시지의 JSON 구조를 정의한다.
 * Kafka로 보내기 전에 ObjectMapper가 이 클래스를 JSON 문자열로 직렬화한다.
 *
 * 수신자 결정 방식:
 *   창고 1개당 WH_MANAGER 1명을 기본 가정으로 두고,
 *   WMS는 선택된 창고의 managerAccountId를 managerId 필드에 담아 보낸다.
 *
 * 발행 측(wms-service)이 전송해야 하는 JSON 예시:
 * {
 *   "asnId": "ASN-2026-001",
 *   "managerId": "1001",
 *   "asnCount": 1,
 *   "expectedDate": "2026-04-10",
 *   "timestamp": "2026-04-05T10:00:00"
 * }
 *
 * 비즈니스 로직 연결 가이드:
 *   추천 연결 지점은 RegisterAsnService.register(...)의 ASN 헤더/품목 저장이 모두 끝난 직후다.
 *   저장이 끝난 뒤 아래 데이터를 모아 발행한다.
 *   - asnId: RegisterAsnCommand.getAsnId()
 *   - managerId: WarehouseManagerAssignmentRepository.findByWarehouseIdAndTenantId(...)로 조회한 managerAccountId
 *   - asnCount: 현재 단건 ASN 등록 흐름이라면 1, 추후 배치 등록이 생기면 실제 등록 건수로 확장
 *   - expectedDate: RegisterAsnCommand.getExpectedDate().toString()
 *   - timestamp: WMS 서버 기준 LocalDateTime.now()
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsnCreatedEvent {

    /** 등록된 ASN의 고유 ID */
    private String asnId;

    /**
     * 알림 수신 대상 WH_MANAGER의 accountId
     * 창고 관리자 스냅샷의 managerAccountId를 문자열로 전달한다.
     */
    private String managerId;

    /** 등록된 입고예정 건수 */
    private int asnCount;

    /** 입고 예정일 (예: "2026-04-10") */
    private String expectedDate;

    /** 이벤트 발생 시각 */
    private LocalDateTime timestamp;

    // Jackson이 직렬화/역직렬화 보조 처리할 때 기본 생성자가 있으면 테스트와 확장에 유리하다.
    public AsnCreatedEvent() {
    }

    public String getAsnId() {
        return asnId;
    }

    public String getManagerId() {
        return managerId;
    }

    public int getAsnCount() {
        return asnCount;
    }

    public String getExpectedDate() {
        return expectedDate;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setAsnId(String asnId) {
        this.asnId = asnId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public void setAsnCount(int asnCount) {
        this.asnCount = asnCount;
    }

    public void setExpectedDate(String expectedDate) {
        this.expectedDate = expectedDate;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
