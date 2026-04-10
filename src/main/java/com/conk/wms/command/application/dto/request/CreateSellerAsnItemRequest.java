package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateSellerAsnItemRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// Seller ASN 생성 요청의 품목 한 줄.
public class CreateSellerAsnItemRequest {
    private String sku;
    private String productName;
    private int quantity;
    private int cartons;
}

