package com.conk.wms.command.application.dto.response;

import lombok.Getter;

/**
 * CreateSellerAsnResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
// Seller ASN 등록 성공 시 생성된 ASN 번호를 반환한다.
public class CreateSellerAsnResponse {
    private final String id;

    public CreateSellerAsnResponse(String id) {
        this.id = id;
    }
}

