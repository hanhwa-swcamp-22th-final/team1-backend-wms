package com.conk.wms.query.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * value/label 쌍으로 내려주는 공용 옵션 응답 DTO다.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class OptionItemResponse {

    private String value;
    private String label;
}
