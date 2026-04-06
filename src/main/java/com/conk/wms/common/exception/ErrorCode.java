package com.conk.wms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전반에서 사용하는 비즈니스 에러 코드와 메시지를 모아둔 enum이다.
 */
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
    ASN_WORKER_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "ASN-033", "입고 작업을 찾을 수 없습니다."),
    ASN_WORK_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "ASN-034", "입고 작업 상세를 찾을 수 없습니다."),
    ASN_WORK_STAGE_INVALID(HttpStatus.BAD_REQUEST, "ASN-035", "지원하지 않는 입고 작업 단계입니다."),
    ASN_PUTAWAY_NOT_READY(HttpStatus.CONFLICT, "ASN-036", "검수가 완료되지 않아 적재를 진행할 수 없습니다."),

    OUTBOUND_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-001", "출고 대상 주문을 찾을 수 없습니다."),
    OUTBOUND_DISPATCH_NOT_ALLOWED(HttpStatus.CONFLICT, "OUTBOUND-002", "현재 상태에서는 출고 지시를 진행할 수 없습니다."),
    OUTBOUND_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "OUTBOUND-003", "출고 가능한 재고가 부족합니다."),
    OUTBOUND_ORDER_IDS_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-004", "출고 지시할 주문은 1건 이상이어야 합니다."),
    OUTBOUND_ALREADY_DISPATCHED(HttpStatus.CONFLICT, "OUTBOUND-005", "이미 출고 지시된 주문입니다."),
    OUTBOUND_ASSIGNMENT_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-006", "작업 배정 대상 주문을 찾을 수 없습니다."),
    OUTBOUND_WORKER_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-007", "작업자 계정은 필수입니다."),
    OUTBOUND_PICKING_LIST_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-008", "피킹 리스트를 찾을 수 없습니다."),
    OUTBOUND_WORKER_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-009", "작업자 작업을 찾을 수 없습니다."),
    OUTBOUND_WORK_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-010", "작업 상세를 찾을 수 없습니다."),
    OUTBOUND_WORK_STAGE_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-011", "작업 단계는 필수입니다."),
    OUTBOUND_WORK_STAGE_INVALID(HttpStatus.BAD_REQUEST, "OUTBOUND-012", "지원하지 않는 작업 단계입니다."),
    OUTBOUND_WORK_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "OUTBOUND-013", "실제 처리 수량은 0 이상이어야 합니다."),
    OUTBOUND_PACKING_NOT_READY(HttpStatus.CONFLICT, "OUTBOUND-014", "피킹이 완료되지 않아 패킹을 진행할 수 없습니다."),
    OUTBOUND_INVOICE_NOT_READY(HttpStatus.CONFLICT, "OUTBOUND-015", "패킹이 완료되지 않아 송장을 발행할 수 없습니다."),
    OUTBOUND_INVOICE_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-016", "송장 발행 대상 주문을 찾을 수 없습니다."),
    OUTBOUND_INVOICE_ORDER_IDS_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-017", "송장 발행할 주문은 1건 이상이어야 합니다."),
    OUTBOUND_CONFIRM_NOT_READY(HttpStatus.CONFLICT, "OUTBOUND-018", "패킹 및 송장이 완료되지 않아 출고 확정을 진행할 수 없습니다."),
    OUTBOUND_CONFIRM_ALREADY_COMPLETED(HttpStatus.CONFLICT, "OUTBOUND-019", "이미 출고 확정된 주문입니다."),
    OUTBOUND_CONFIRM_ORDER_IDS_REQUIRED(HttpStatus.BAD_REQUEST, "OUTBOUND-020", "출고 확정할 주문은 1건 이상이어야 합니다."),
    OUTBOUND_CONFIRM_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-021", "출고 확정 대상 주문을 찾을 수 없습니다."),
    OUTBOUND_CONFIRM_ALLOCATED_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTBOUND-022", "마감할 ALLOCATED 재고를 찾을 수 없습니다."),

    WORKER_ACCOUNT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "WORKER-001", "작업자 코드는 필수입니다."),
    WORKER_ACCOUNT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "WORKER-002", "작업자 이름은 필수입니다."),
    WORKER_ACCOUNT_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "WORKER-003", "초기 비밀번호는 필수입니다."),
    WORKER_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKER-004", "작업자 계정을 찾을 수 없습니다."),
    WORKER_ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "WORKER-005", "이미 존재하는 작업자 계정입니다."),
    BIN_ASSIGNMENT_BIN_REQUIRED(HttpStatus.BAD_REQUEST, "WORKER-006", "배정할 Bin은 필수입니다."),
    BIN_ASSIGNMENT_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKER-007", "배정 대상 Bin을 찾을 수 없습니다."),
    BIN_ASSIGNMENT_WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKER-008", "배정 대상 작업자를 찾을 수 없습니다."),

    WAREHOUSE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "WH-001", "창고명은 필수입니다."),
    WAREHOUSE_ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "WH-002", "창고 주소는 필수입니다."),
    WAREHOUSE_AREA_INVALID(HttpStatus.BAD_REQUEST, "WH-003", "창고 면적은 1 이상이어야 합니다."),
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "WH-004", "창고를 찾을 수 없습니다."),
    WAREHOUSE_MANAGER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "WH-005", "담당 관리자 이름은 필수입니다."),
    WAREHOUSE_MANAGER_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "WH-006", "담당 관리자 이메일은 필수입니다.");

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
