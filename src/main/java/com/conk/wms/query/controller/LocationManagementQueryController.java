package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.query.controller.dto.response.LocationZoneResponse;
import com.conk.wms.query.service.GetLocationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 창고 배치도와 Bin 배정 화면에서 사용하는 location 트리 조회를 담당하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/locations", "/wh_locations"})
public class LocationManagementQueryController {

    private final GetLocationsService getLocationsService;

    public LocationManagementQueryController(GetLocationsService getLocationsService) {
        this.getLocationsService = getLocationsService;
    }

    @GetMapping
    public ResponseEntity<List<LocationZoneResponse>> getLocations(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(getLocationsService.getLocations(resolveTenantId(authContext)));
    }
}
