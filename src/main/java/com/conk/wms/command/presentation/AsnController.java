package com.conk.wms.command.presentation;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.presentation.ApiResponse;
import com.conk.wms.command.application.RegisterAsnService;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.conk.wms.query.application.GetSellerAsnListService;
import com.conk.wms.query.application.dto.SellerAsnListItemResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Seller ASN 등록/목록 조회 전용 진입점.
// 상세/관리자용 ASN 조회는 이후 별도 API로 분리해서 확장할 예정이라,
// 현재는 seller 화면에서 실제로 쓰는 `/wms/seller/asns`만 먼저 담당한다.
@RestController
@RequestMapping("/wms/seller/asns")
public class AsnController {

    private final RegisterAsnService registerAsnService;
    private final GetSellerAsnListService getSellerAsnListService;

    public AsnController(RegisterAsnService registerAsnService, GetSellerAsnListService getSellerAsnListService) {
        this.registerAsnService = registerAsnService;
        this.getSellerAsnListService = getSellerAsnListService;
    }

    // Seller ASN 목록 화면의 row shape를 그대로 내려준다.
    // tenant header를 현재는 seller 식별값처럼 사용하고 있으므로 목록도 같은 기준으로 필터링한다.
    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerAsnListItemResponse>>> getSellerAsns(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        List<SellerAsnListItemResponse> response = getSellerAsnListService.getSellerAsns(sellerId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    // 프론트 create 화면 payload를 command 모델로 변환하는 경계.
    // 물류 부가정보(originCountry 등)는 현재 ERD 저장 컬럼이 없어 request에서는 받되 저장에는 사용하지 않는다.
    @PostMapping
    public ResponseEntity<ApiResponse<CreateSellerAsnResponse>> register(
            @RequestBody CreateSellerAsnRequest request,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        List<RegisterAsnItemCommand> items = toItemCommands(request);
        String sellerId = resolveSellerId(tenantCode);

        registerAsnService.register(new RegisterAsnCommand(
                request.getAsnNo(),
                request.getWarehouseId(),
                sellerId,
                request.getExpectedDate(),
                request.getNote(),
                items
        ));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("created", new CreateSellerAsnResponse(request.getAsnNo())));
    }

    // 프론트는 `detail.items[]` 구조로 보내므로 서비스 command용 flat item 목록으로 한 번 변환한다.
    private List<RegisterAsnItemCommand> toItemCommands(CreateSellerAsnRequest request) {
        if (request == null || request.getDetail() == null || request.getDetail().getItems() == null) {
            return List.of();
        }

        return request.getDetail().getItems().stream()
                .map(item -> new RegisterAsnItemCommand(
                        item.getSku(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getCartons()
                ))
                .toList();
    }

    // 아직 auth/security가 붙지 않은 상태라 seller 식별값은 임시로 X-Tenant-Code에서 꺼낸다.
    // 이후 JWT/인증 연동 시 이 메서드가 security context 조회로 대체될 수 있다.
    private String resolveSellerId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateSellerAsnRequest {
        // 프론트 mock payload와 최대한 호환되도록 필드를 넓게 받는다.
        // 실제 저장에 쓰는 값은 asnNo, warehouseId, expectedDate, note, detail.items 위주다.
        private String asnNo;
        private String warehouseId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expectedDate;
        private String note;
        private String originCountry;
        private String senderAddress;
        private String senderPhone;
        private String shippingMethod;
        private DetailRequest detail;

        public String getAsnNo() {
            return asnNo;
        }

        public void setAsnNo(String asnNo) {
            this.asnNo = asnNo;
        }

        public String getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
        }

        public LocalDate getExpectedDate() {
            return expectedDate;
        }

        public void setExpectedDate(LocalDate expectedDate) {
            this.expectedDate = expectedDate;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getOriginCountry() {
            return originCountry;
        }

        public void setOriginCountry(String originCountry) {
            this.originCountry = originCountry;
        }

        public String getSenderAddress() {
            return senderAddress;
        }

        public void setSenderAddress(String senderAddress) {
            this.senderAddress = senderAddress;
        }

        public String getSenderPhone() {
            return senderPhone;
        }

        public void setSenderPhone(String senderPhone) {
            this.senderPhone = senderPhone;
        }

        public String getShippingMethod() {
            return shippingMethod;
        }

        public void setShippingMethod(String shippingMethod) {
            this.shippingMethod = shippingMethod;
        }

        public DetailRequest getDetail() {
            return detail;
        }

        public void setDetail(DetailRequest detail) {
            this.detail = detail;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetailRequest {
        private List<ItemRequest> items;

        public List<ItemRequest> getItems() {
            return items;
        }

        public void setItems(List<ItemRequest> items) {
            this.items = items;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemRequest {
        // seller create 화면의 item 한 줄에 대응한다.
        private String sku;
        private String productName;
        private int quantity;
        private int cartons;

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getCartons() {
            return cartons;
        }

        public void setCartons(int cartons) {
            this.cartons = cartons;
        }
    }

    public static class CreateSellerAsnResponse {
        private final String id;

        public CreateSellerAsnResponse(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
