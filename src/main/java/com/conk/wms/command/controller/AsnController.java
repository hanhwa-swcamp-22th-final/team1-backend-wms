package com.conk.wms.command.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.command.service.RegisterAsnService;
import com.conk.wms.command.dto.RegisterAsnCommand;
import com.conk.wms.command.dto.RegisterAsnItemCommand;
import com.conk.wms.command.controller.dto.request.CreateSellerAsnRequest;
import com.conk.wms.command.controller.dto.response.CreateSellerAsnResponse;
import com.conk.wms.query.service.GetSellerAsnListService;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
