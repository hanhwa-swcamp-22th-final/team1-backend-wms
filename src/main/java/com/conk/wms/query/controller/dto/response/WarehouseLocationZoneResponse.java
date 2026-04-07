package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 상세 화면의 로케이션 드로어에서 사용하는 Zone 트리 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseLocationZoneResponse {

    private String zone;
    private String label;
    private int utilPct;
    private int available;
    private int total;
    private List<WarehouseLocationRackResponse> racks;

    @Getter
    @Builder
    public static class WarehouseLocationRackResponse {
        private String name;
        private List<WarehouseLocationBinResponse> bins;
    }

    @Getter
    @Builder
    public static class WarehouseLocationBinResponse {
        private String id;
        private String state;
    }
}
