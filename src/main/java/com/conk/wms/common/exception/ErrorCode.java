package com.conk.wms.common.exception;

import org.springframework.http.HttpStatus;

// 공통 비즈니스 에러 코드 모음.
// 현재는 ASN 흐름부터 적용했고, 이후 다른 서비스도 같은 패턴으로 확장할 수 있다.
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
    ASN_DUPLICATE_SKU(HttpStatus.BAD_REQUEST, "ASN-012", "중복된 SKU 입니다."),
    ASN_NOT_FOUND(HttpStatus.NOT_FOUND, "ASN-013", "ASN 정보를 찾을 수 없습니다."),
    ASN_ARRIVAL_NOT_ALLOWED(HttpStatus.CONFLICT, "ASN-014", "현재 상태에서는 도착 확인을 할 수 없습니다."),
    ASN_INSPECTION_NOT_ALLOWED(HttpStatus.CONFLICT, "ASN-015", "현재 상태에서는 검수/적재를 진행할 수 없습니다."),
    ASN_INSPECTION_ITEMS_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-016", "검수/적재 품목은 1개 이상이어야 합니다."),
    ASN_INSPECTION_SKU_NOT_FOUND(HttpStatus.BAD_REQUEST, "ASN-017", "ASN에 없는 SKU 입니다."),
    ASN_INSPECTION_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ASN-018", "검수/적재 수량은 0 이상이어야 합니다."),
    ASN_PUTAWAY_LOCATION_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-019", "적재 수량이 있으면 locationId는 필수입니다."),
    ASN_INSPECTION_COMPLETE_INVALID(HttpStatus.BAD_REQUEST, "ASN-020", "검수/적재 완료 조건이 맞지 않습니다."),
    ASN_INSPECTION_RESULT_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-021", "검수/적재 데이터가 없습니다."),
    ASN_BIN_MATCH_SKU_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-022", "추천 Bin 조회를 위한 SKU는 필수입니다."),
    ASN_PUTAWAY_ASSIGN_NOT_ALLOWED(HttpStatus.CONFLICT, "ASN-023", "현재 상태에서는 Bin 배정을 진행할 수 없습니다."),
    ASN_PUTAWAY_ITEMS_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-024", "Bin 배정 품목은 1개 이상이어야 합니다."),
    ASN_LOCATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "ASN-025", "존재하지 않는 location 입니다."),
    ASN_LOCATION_WAREHOUSE_MISMATCH(HttpStatus.BAD_REQUEST, "ASN-026", "ASN과 다른 창고의 location 입니다."),
    ASN_LOCATION_INACTIVE(HttpStatus.BAD_REQUEST, "ASN-027", "비활성 location 입니다."),
    ASN_LOCATION_ALREADY_OCCUPIED(HttpStatus.BAD_REQUEST, "ASN-028", "다른 SKU가 사용 중인 location 입니다."),
    ASN_LOCATION_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "ASN-029", "location 수용 가능 수량을 초과했습니다."),
    ASN_CONFIRM_NOT_ALLOWED(HttpStatus.CONFLICT, "ASN-030", "현재 상태에서는 입고 확정을 진행할 수 없습니다."),
    ASN_CONFIRM_RESULT_REQUIRED(HttpStatus.BAD_REQUEST, "ASN-031", "입고 확정을 위한 검수/적재 데이터가 없습니다."),
    ASN_CONFIRM_INCOMPLETE(HttpStatus.BAD_REQUEST, "ASN-032", "완료되지 않은 검수/적재 데이터가 있습니다."),

    OUTBOUND_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-001", "출고 대상 주문을 찾을 수 없습니다."),
    OUTBOUND_DISPATCH_NOT_ALLOWED(HttpStatus.CONFLICT, "OUTBOUND-002", "현재 상태에서는 출고 지시를 진행할 수 없습니다."),
    OUTBOUND_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "OUTBOUND-003", "출고 가능한 재고가 부족합니다."),
    OUTBOUND_ORDER_IDS_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-004", "출고 지시할 주문은 1건 이상이어야 합니다."),
    OUTBOUND_ALREADY_DISPATCHED(HttpStatus.CONFLICT, "OUTBOUND-005", "이미 출고 지시된 주문입니다."),
    OUTBOUND_ASSIGNMENT_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-006", "작업 배정 대상 주문을 찾을 수 없습니다."),
    OUTBOUND_WORKER_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-007", "작업자 계정은 필수입니다."),
    OUTBOUND_PICKING_LIST_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-008", "피킹 리스트를 찾을 수 없습니다.");

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
