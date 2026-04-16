package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 총괄 관리자 ASN 목록과 페이지/필터 메타데이터를 함께 표현하는 DTO다.
 */
@Getter
@Builder
public class MasterAsnListResponse {

    private List<MasterAsnListItemResponse> items;
    private long total;
    private int page;
    private int size;
    private Map<String, Long> counts;
    private List<OptionResponse> warehouseOptions;
    private List<OptionResponse> companyOptions;

    @Getter
    @Builder
    public static class OptionResponse {
        private String label;
        private String value;
    }
}
