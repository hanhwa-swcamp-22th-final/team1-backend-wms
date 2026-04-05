package com.conk.wms.command.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SaveAsnInspectionRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// 검수/적재 저장 요청.
// SKU별 검수 수량과 불량, 적재 위치/적재 수량을 함께 받는다.
public class SaveAsnInspectionRequest {

    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemRequest {
        private String skuId;
        private String locationId;
        private Integer inspectedQuantity;
        private Integer defectiveQuantity;
        private String defectReason;
        private Integer putawayQuantity;
    }
}
