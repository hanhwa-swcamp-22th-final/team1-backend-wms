package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.OrderIdsRequestDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;
import com.conk.wms.query.client.feign.IntegrationServiceFeignClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * integration-service Feign 어댑터다.
 */
@Component
@ConditionalOnProperty(name = "wms.stub-clients.enabled", havingValue = "false", matchIfMissing = true)
public class FeignIntegrationServiceClient implements IntegrationServiceClient {

    private final IntegrationServiceFeignClient integrationServiceFeignClient;

    public FeignIntegrationServiceClient(IntegrationServiceFeignClient integrationServiceFeignClient) {
        this.integrationServiceFeignClient = integrationServiceFeignClient;
    }

    @Override
    public ShipmentRecommendationDto recommendShipment(String tenantCode, IssueLabelRequestDto request) {
        return Optional.ofNullable(integrationServiceFeignClient.recommendLabel(tenantCode, request))
                .map(response -> response.getData())
                .orElse(null);
    }

    @Override
    public ShipmentInvoiceDto issueLabel(String tenantCode, IssueLabelRequestDto request) {
        return Optional.ofNullable(integrationServiceFeignClient.issueLabel(tenantCode, request))
                .map(response -> response.getData())
                .orElse(null);
    }

    @Override
    public Map<String, ShipmentInvoiceDto> getShipmentInvoices(String tenantCode, List<String> orderIds) {
        return Optional.ofNullable(integrationServiceFeignClient.getLabels(
                        tenantCode,
                        OrderIdsRequestDto.builder().orderIds(orderIds).build()))
                .map(response -> response.getData() == null ? Map.<String, ShipmentInvoiceDto>of() : response.getData())
                .orElseGet(Map::of);
    }
}
