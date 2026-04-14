package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Seller ASN 목록과 페이지 메타데이터를 함께 표현하는 DTO다.
 */
@Getter
@Builder
public class SellerAsnListResponse {
    private List<SellerAsnListItemResponse> items;
    private long total;
    private int page;
    private int size;
}
