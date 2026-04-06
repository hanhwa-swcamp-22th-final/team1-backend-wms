package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 배치도와 Bin 배정 모달에서 쓰는 BIN 단위 응답 DTO다.
 */
@Getter
@Builder
public class LocationBinResponse {

    private String id;
    private String bin;
    private int capacity;
    private int usedQty;
    private String status;
}
