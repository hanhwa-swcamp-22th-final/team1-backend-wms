package com.conk.wms.query.client.feign;

import com.conk.wms.common.config.FeignConfig;
import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.OrderIdsRequestDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;
import com.conk.wms.query.client.support.ServiceApiResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * integration-service 내부 API용 Feign 클라이언트다.
 */
@FeignClient(
        name = "integrationServiceFeignClient",
        url = "${wms.clients.integration.base-url:http://localhost:8084}",
        configuration = FeignConfig.class
)
public interface IntegrationServiceFeignClient {

    @PostMapping("/integrations/internal/labels")
    ServiceApiResponse<ShipmentInvoiceDto> issueLabel(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody IssueLabelRequestDto request
    );

    @PostMapping("/integrations/internal/labels/recommendation")
    ServiceApiResponse<ShipmentRecommendationDto> recommendLabel(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody IssueLabelRequestDto request
    );

    @PostMapping("/integrations/internal/labels/batch-query")
    ServiceApiResponse<Map<String, ShipmentInvoiceDto>> getLabels(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody OrderIdsRequestDto request
    );
}
