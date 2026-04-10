package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * CreateSellerAsnRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// Seller ASN 생성 요청의 최상위 HTTP body.
// 프론트 mock payload와 호환되도록 부가 필드도 함께 받지만, 실제 저장은 핵심 필드만 사용한다.
public class CreateSellerAsnRequest {
    private String asnNo;
    private String warehouseId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedDate;
    private String note;
    private String originCountry;
    private String senderAddress;
    private String senderPhone;
    private String shippingMethod;
    private CreateSellerAsnDetailRequest detail;
}

