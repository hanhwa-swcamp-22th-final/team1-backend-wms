package com.conk.wms.command.infrastructure.kafka;

/**
 * Kafka 토픽 이름 상수 모음이다.
 *
 * 토픽 문자열을 서비스 코드 곳곳에 하드코딩하면
 * 오타, 이름 변경 누락, 테스트 중복 정의가 발생하기 쉽다.
 * 따라서 발행 측(WMS)은 이 클래스의 상수만 참조하도록 통일한다.
 */
public final class KafkaTopics {

    /** 작업 배정 알림 이벤트 토픽 */
    public static final String WMS_TASK_ASSIGNED = "wms.task.assigned";

    /** 입고예정 등록 알림 이벤트 토픽 */
    public static final String WMS_ASN_CREATED = "wms.asn.created";

    private KafkaTopics() {
    }
}
