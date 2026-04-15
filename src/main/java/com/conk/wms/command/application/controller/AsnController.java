package com.conk.wms.command.application.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.command.application.service.RegisterAsnService;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.conk.wms.command.application.dto.request.CreateSellerAsnRequest;
import com.conk.wms.command.application.dto.response.CreateSellerAsnResponse;
import com.conk.wms.query.controller.dto.response.SellerAsnListResponse;
import com.conk.wms.query.controller.dto.response.SellerAsnOptionsResponse;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import com.conk.wms.query.service.GetSellerAsnListService;
import com.conk.wms.query.service.GetSellerAsnOptionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;

/**
 * 셀러가 ASN을 등록할 때 사용하는 command API 컨트롤러다.
 * 입고 요청 생성 흐름의 시작점을 담당한다.
 */
// Seller ASN 등록/목록 조회 전용 진입점.
// 상세/관리자용 ASN 조회는 이후 별도 API로 분리해서 확장할 예정이라,
// 현재는 seller 화면에서 실제로 쓰는 `/wms/seller/asns`만 먼저 담당한다.
@RestController
@RequestMapping("/wms/seller/asns")
public class AsnController {

    private final RegisterAsnService registerAsnService;
    private final GetSellerAsnListService getSellerAsnListService;
    private final GetSellerAsnOptionsService getSellerAsnOptionsService;

    public AsnController(RegisterAsnService registerAsnService,
                         GetSellerAsnListService getSellerAsnListService,
                         GetSellerAsnOptionsService getSellerAsnOptionsService) {
        this.registerAsnService = registerAsnService;
        this.getSellerAsnListService = getSellerAsnListService;
        this.getSellerAsnOptionsService = getSellerAsnOptionsService;
    }

    // Seller ASN 목록 화면의 row shape를 그대로 내려준다.
    // tenant header를 현재는 seller 식별값처럼 사용하고 있으므로 목록도 같은 기준으로 필터링한다.
    @GetMapping
    public ResponseEntity<ApiResponse<SellerAsnListResponse>> getSellerAsns(
            AuthContext authContext, @RequestParam("page") int page, @RequestParam("size") int size
    ) {
        String sellerId = resolveSellerId(authContext);
        SellerAsnListResponse response = getSellerAsnListService.getSellerAsns(sellerId, page, size);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<SellerAsnOptionsResponse>> getSellerAsnOptions(
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerAsnOptionsService.getOptions(sellerId)));
    }

    // 프론트 create 화면 payload를 command 모델로 변환하는 경계.
    // 물류 부가정보(originCountry 등)는 현재 ERD 저장 컬럼이 없어 request에서는 받되 저장에는 사용하지 않는다.
    @PostMapping
    public ResponseEntity<ApiResponse<CreateSellerAsnResponse>> register(
            @RequestBody CreateSellerAsnRequest request,
            AuthContext authContext
    ) {
        List<RegisterAsnItemCommand> items = toItemCommands(request);
        String sellerId = resolveSellerId(authContext);

        String asnId = registerAsnService.register(new RegisterAsnCommand(
                request.getAsnNo(),
                request.getWarehouseId(),
                sellerId,
                request.getExpectedDate(),
                request.getNote(),
                items
        ));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("created", new CreateSellerAsnResponse(asnId)));
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

}





