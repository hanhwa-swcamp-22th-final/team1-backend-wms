package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 셀러 상품 등록/수정 화면 옵션 응답 DTO다.
 */
@Getter
@Builder
public class SellerProductOptionsResponse {

    private List<OptionItemResponse> categories;
    private List<OptionItemResponse> hsCodes;
    private List<OptionItemResponse> originCountries;
}
