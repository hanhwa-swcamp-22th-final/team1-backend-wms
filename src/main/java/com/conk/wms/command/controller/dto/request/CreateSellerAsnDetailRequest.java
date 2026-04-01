package com.conk.wms.command.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// Seller ASN 생성 요청의 detail 블록.
public class CreateSellerAsnDetailRequest {
    private List<CreateSellerAsnItemRequest> items;
}
