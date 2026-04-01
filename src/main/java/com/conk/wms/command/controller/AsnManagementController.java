package com.conk.wms.command.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.controller.dto.request.ConfirmAsnArrivalRequest;
import com.conk.wms.command.controller.dto.response.ConfirmAsnArrivalResponse;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.dto.ConfirmAsnArrivalCommand;
import com.conk.wms.command.service.ConfirmAsnArrivalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ASN 운영 액션 전용 컨트롤러.
// seller 등록/조회 API와 분리해서, 이후 도착 확인·작업자 배정·검수 같은 창고 운영 액션을 이쪽에 모은다.
@RestController
@RequestMapping("/wms/asns")
public class AsnManagementController {

    private final ConfirmAsnArrivalService confirmAsnArrivalService;

    public AsnManagementController(ConfirmAsnArrivalService confirmAsnArrivalService) {
        this.confirmAsnArrivalService = confirmAsnArrivalService;
    }

    // 아직 인증이 없어 manager 식별값도 임시로 tenant 헤더에서 꺼낸다.
    // 추후 security 도입 시 actor는 JWT claim(sub/role) 기반으로 교체한다.
    @PatchMapping("/{asnId}/arrival")
    public ResponseEntity<ApiResponse<ConfirmAsnArrivalResponse>> confirmArrival(
            @PathVariable String asnId,
            @RequestBody(required = false) ConfirmAsnArrivalRequest request,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String actorId = resolveActorId(tenantCode);
        Asn asn = confirmAsnArrivalService.confirm(new ConfirmAsnArrivalCommand(
                asnId,
                request != null ? request.getArrivedAt() : null,
                actorId
        ));

        ConfirmAsnArrivalResponse response = new ConfirmAsnArrivalResponse(
                asn.getAsnId(),
                asn.getStatus(),
                asn.getArrivedAt()
        );
        return ResponseEntity.ok(ApiResponse.success("arrival confirmed", response));
    }

    private String resolveActorId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }
}
