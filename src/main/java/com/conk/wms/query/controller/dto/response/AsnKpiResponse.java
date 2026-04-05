package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * AsnKpiResponse 조회 응답 모델을 표현하는 DTO다.
 */
@Getter
@Builder
// ASN 목록 화면 상단 KPI 응답 DTO.
// 현재는 seller 범위 기준으로 total/submitted/received/cancelled 4개 수치만 내려준다.
public class AsnKpiResponse {
    // 전체 ASN 수
    private int total;
    // seller 화면 기준 제출됨
    private int submitted;
    // seller 화면 기준 입고완료
    private int received;
    // seller 화면 기준 취소
    private int cancelled;
}
