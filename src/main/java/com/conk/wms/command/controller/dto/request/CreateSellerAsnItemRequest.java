package com.conk.wms.command.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
