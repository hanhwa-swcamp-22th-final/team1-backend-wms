package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;

import java.util.List;
import java.util.Map;

/**
 * integration-service와의 송장 발행/조회 연동을 추상화한 포트다.
 */
public interface IntegrationServiceClient {

    ShipmentRecommendationDto recommendShipment(String tenantCode, IssueLabelRequestDto request);

    ShipmentInvoiceDto issueLabel(String tenantCode, IssueLabelRequestDto request);

    Map<String, ShipmentInvoiceDto> getShipmentInvoices(String tenantCode, List<String> orderIds);
}
