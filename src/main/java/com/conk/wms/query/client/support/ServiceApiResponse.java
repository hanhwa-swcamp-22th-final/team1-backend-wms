package com.conk.wms.query.client.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내부 서비스 공통 응답 래퍼다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
}
