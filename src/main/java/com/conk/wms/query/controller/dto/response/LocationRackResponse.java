package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 배치도 화면용 Rack 묶음을 표현하는 응답 DTO다.
 */
@Getter
@Builder
public class LocationRackResponse {

    private String rack;
    private List<LocationBinResponse> bins;
}
