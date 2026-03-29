package com.conk.wms.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    TENANT_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "COMMON-001", "X-Tenant-Code 헤더가 필요합니다."),

    ASN_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-001", "ASN 번호는 필수입니다."),
    ASN_WAREHOUSE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-002", "창고 ID는 필수입니다."),
    ASN_SELLER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-003", "셀러 식별 정보가 없습니다."),
    ASN_EXPECTED_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-004", "예정 입고일은 필수입니다."),
    ASN_SELLER_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "ASN-005", "셀러 메모는 500자를 초과할 수 없습니다."),
    ASN_ITEMS_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-006", "입고 품목은 1개 이상이어야 합니다."),
    ASN_SKU_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-007", "SKU는 필수입니다."),
    ASN_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ASN-008", "입고 수량은 1 이상이어야 합니다."),
    ASN_INVALID_BOX_QUANTITY(HttpStatus.BAD_REQUEST, "ASN-009", "박스 수는 1 이상이어야 합니다."),
    ASN_WAREHOUSE_NOT_FOUND(HttpStatus.BAD_REQUEST, "ASN-010", "존재하지 않는 창고입니다."),
    ASN_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "ASN-011", "이미 존재하는 ASN 번호입니다."),
    ASN_DUPLICATE_SKU(HttpStatus.BAD_REQUEST, "ASN-012", "중복된 SKU 입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
