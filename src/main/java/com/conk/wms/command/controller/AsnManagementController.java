package com.conk.wms.command.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.command.controller.dto.request.AssignAsnPutawayRequest;
import com.conk.wms.command.controller.dto.request.ConfirmAsnArrivalRequest;
import com.conk.wms.command.controller.dto.request.SaveAsnInspectionRequest;
import com.conk.wms.command.controller.dto.response.AssignAsnPutawayResponse;
import com.conk.wms.command.controller.dto.response.CompleteAsnInspectionResponse;
import com.conk.wms.command.controller.dto.response.ConfirmAsnArrivalResponse;
import com.conk.wms.command.controller.dto.response.ConfirmAsnInventoryResponse;
import com.conk.wms.command.controller.dto.response.SaveAsnInspectionResponse;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.dto.AssignAsnPutawayCommand;
import com.conk.wms.command.dto.CompleteAsnInspectionCommand;
import com.conk.wms.command.dto.ConfirmAsnArrivalCommand;
import com.conk.wms.command.dto.ConfirmAsnInventoryCommand;
import com.conk.wms.command.dto.SaveAsnInspectionCommand;
import com.conk.wms.command.service.AssignAsnPutawayService;
import com.conk.wms.command.service.CompleteAsnInspectionService;
import com.conk.wms.command.service.ConfirmAsnArrivalService;
import com.conk.wms.command.service.ConfirmAsnInventoryService;
import com.conk.wms.command.service.SaveAsnInspectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveActorId;

/**
 * 창고 관리자 기준 ASN command API를 모아둔 컨트롤러다.
 * 도착 확인, 검수/적재, BIN 배정, 입고 확정 같은 입고 실행 명령을 처리한다.
 */
// ASN 운영 액션 전용 컨트롤러.
// seller 등록/조회 API와 분리해서, 이후 도착 확인·작업자 배정·검수 같은 창고 운영 액션을 이쪽에 모은다.
@RestController
@RequestMapping("/wms/asns")
public class AsnManagementController {

    private final ConfirmAsnArrivalService confirmAsnArrivalService;
    private final AssignAsnPutawayService assignAsnPutawayService;
    private final SaveAsnInspectionService saveAsnInspectionService;
    private final CompleteAsnInspectionService completeAsnInspectionService;
    private final ConfirmAsnInventoryService confirmAsnInventoryService;

    public AsnManagementController(ConfirmAsnArrivalService confirmAsnArrivalService,
                                   AssignAsnPutawayService assignAsnPutawayService,
                                   SaveAsnInspectionService saveAsnInspectionService,
                                   CompleteAsnInspectionService completeAsnInspectionService,
                                   ConfirmAsnInventoryService confirmAsnInventoryService) {
        this.confirmAsnArrivalService = confirmAsnArrivalService;
        this.assignAsnPutawayService = assignAsnPutawayService;
        this.saveAsnInspectionService = saveAsnInspectionService;
        this.completeAsnInspectionService = completeAsnInspectionService;
        this.confirmAsnInventoryService = confirmAsnInventoryService;
    }

    // 아직 인증이 없어 manager 식별값도 임시로 tenant 헤더에서 꺼낸다.
    // 추후 security 도입 시 actor는 JWT claim(sub/role) 기반으로 교체한다.
    @PatchMapping("/{asnId}/arrival")
    public ResponseEntity<ApiResponse<ConfirmAsnArrivalResponse>> confirmArrival(
            @PathVariable String asnId,
            @RequestBody(required = false) ConfirmAsnArrivalRequest request,
            AuthContext authContext
    ) {
        String actorId = resolveActorId(authContext);
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

    // Bin 미배정 ASN 화면에서 SKU별 적재 location만 먼저 확정 저장한다.
    // 이 단계는 상태를 바꾸지 않고 inspection_putaway.locationId만 선저장한다.
    @PatchMapping("/{asnId}/putaway")
    public ResponseEntity<ApiResponse<AssignAsnPutawayResponse>> assignPutaway(
            @PathVariable String asnId,
            @RequestBody AssignAsnPutawayRequest request,
            AuthContext authContext
    ) {
        String actorId = resolveActorId(authContext);
        List<AssignAsnPutawayRequest.ItemRequest> items = request != null && request.getItems() != null
                ? request.getItems()
                : List.of();
        int assignedCount = assignAsnPutawayService.assign(new AssignAsnPutawayCommand(
                asnId,
                actorId,
                items.stream()
                        .map(item -> new AssignAsnPutawayCommand.ItemCommand(
                                item.getSkuId(),
                                item.getLocationId()
                        ))
                        .toList()
        ));

        AssignAsnPutawayResponse response = new AssignAsnPutawayResponse(asnId, assignedCount);
        return ResponseEntity.ok(ApiResponse.success("putaway assigned", response));
    }

    // 검수와 적재 위치/수량을 한 번에 저장한다.
    // 첫 저장이면 ASN 상태를 INSPECTING_PUTAWAY로 올리고, 이후 저장은 같은 상태를 유지한다.
    @PatchMapping("/{asnId}/inspection")
    public ResponseEntity<ApiResponse<SaveAsnInspectionResponse>> saveInspection(
            @PathVariable String asnId,
            @RequestBody SaveAsnInspectionRequest request,
            AuthContext authContext
    ) {
        String actorId = resolveActorId(authContext);
        List<SaveAsnInspectionRequest.ItemRequest> items = request != null && request.getItems() != null
                ? request.getItems()
                : List.of();
        Asn asn = saveAsnInspectionService.save(new SaveAsnInspectionCommand(
                asnId,
                actorId,
                items.stream()
                        .map(item -> new SaveAsnInspectionCommand.ItemCommand(
                                item.getSkuId(),
                                item.getLocationId(),
                                item.getInspectedQuantity() != null ? item.getInspectedQuantity() : 0,
                                item.getDefectiveQuantity() != null ? item.getDefectiveQuantity() : 0,
                                item.getDefectReason(),
                                item.getPutawayQuantity() != null ? item.getPutawayQuantity() : 0
                        ))
                        .toList()
        ));

        SaveAsnInspectionResponse response = new SaveAsnInspectionResponse(
                asn.getAsnId(),
                asn.getStatus(),
                items.size()
        );
        return ResponseEntity.ok(ApiResponse.success("inspection saved", response));
    }

    // 검수/적재 입력이 모두 끝났는지 검증한 뒤 inspection_putaway row를 완료 상태로 확정한다.
    // 재고 반영은 다음 단계라 ASN은 아직 STORED로 바꾸지 않는다.
    @PatchMapping("/{asnId}/inspection/complete")
    public ResponseEntity<ApiResponse<CompleteAsnInspectionResponse>> completeInspection(
            @PathVariable String asnId,
            AuthContext authContext
    ) {
        String actorId = resolveActorId(authContext);
        CompleteAsnInspectionService.CompleteResult result = completeAsnInspectionService.complete(
                new CompleteAsnInspectionCommand(asnId, actorId)
        );

        CompleteAsnInspectionResponse response = new CompleteAsnInspectionResponse(
                result.getAsn().getAsnId(),
                result.getAsn().getStatus(),
                result.getCompletedItemCount(),
                result.getCompletedAt()
        );
        return ResponseEntity.ok(ApiResponse.success("inspection completed", response));
    }

    // 검수/적재 완료 후 정상 적재 수량을 inventory에 반영하고 ASN을 STORED로 마감한다.
    @PatchMapping("/{asnId}/confirm")
    public ResponseEntity<ApiResponse<ConfirmAsnInventoryResponse>> confirmInventory(
            @PathVariable String asnId,
            AuthContext authContext
    ) {
        String actorId = resolveActorId(authContext);
        ConfirmAsnInventoryService.ConfirmResult result = confirmAsnInventoryService.confirm(
                new ConfirmAsnInventoryCommand(asnId, actorId)
        );

        ConfirmAsnInventoryResponse response = new ConfirmAsnInventoryResponse(
                result.getAsn().getAsnId(),
                result.getAsn().getStatus(),
                result.getAsn().getStoredAt(),
                result.getReflectedInventoryCount()
        );
        return ResponseEntity.ok(ApiResponse.success("inventory confirmed", response));
    }

}
