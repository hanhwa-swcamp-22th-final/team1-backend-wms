package com.conk.wms.query.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 프론트 ASN 상세 모달의 Bin 후보 맵 응답 DTO다.
 */
@Getter
@Builder
public class AsnBinCandidatesResponse {

    private Map<String, List<CandidateResponse>> candidatesBySku;

    @Getter
    @AllArgsConstructor
    public static class CandidateResponse {
        private String bin;
    }
}
