package com.conk.wms.command.presentation;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.presentation.ApiResponse;
import com.conk.wms.command.application.RegisterAsnService;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/wms/seller/asns")
public class AsnController {

    private final RegisterAsnService registerAsnService;

    public AsnController(RegisterAsnService registerAsnService) {
        this.registerAsnService = registerAsnService;
    }

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

    private String resolveSellerId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateSellerAsnRequest {
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
