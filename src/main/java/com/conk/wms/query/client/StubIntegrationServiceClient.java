package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * integration-service 실연동 전 송장 화면과 출고 확정 흐름을 개발하기 위한 임시 stub 구현이다.
 */
@Component
public class StubIntegrationServiceClient implements IntegrationServiceClient {

    private final Map<String, ShipmentInvoiceDto> issuedInvoices = new ConcurrentHashMap<>();

    @Override
    public ShipmentRecommendationDto recommendShipment(String tenantCode, String orderId) {
        int seed = Math.abs(orderId.hashCode());
        String carrier = switch (seed % 3) {
            case 0 -> "UPS";
            case 1 -> "USPS";
            default -> "FedEx";
        };
        String service = switch (carrier) {
            case "UPS" -> "Ground";
            case "USPS" -> "Priority Mail";
            default -> "Express Saver";
        };
        double estimatedRate = 7.5 + (seed % 5) * 1.15;
        double weightLbs = 2.0 + (seed % 4);
        return ShipmentRecommendationDto.builder()
                .recommendedCarrier(carrier)
                .recommendedService(service)
                .estimatedRate(estimatedRate)
                .weightLbs(weightLbs)
                .build();
    }

    @Override
    public ShipmentInvoiceDto issueLabel(String tenantCode, IssueLabelRequestDto request) {
        ShipmentInvoiceDto existing = issuedInvoices.get(key(tenantCode, request.getOrderId()));
        if (existing != null) {
            return existing;
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        ShipmentInvoiceDto invoice = ShipmentInvoiceDto.builder()
                .orderId(request.getOrderId())
                .invoiceNo("INV-" + request.getOrderId())
                .trackingCode("TRK-" + request.getOrderId())
                .carrierType(request.getCarrier())
                .service(request.getService())
                .freightChargeAmt(1250)
                .shipToAddress("Seoul, KR")
                .trackingUrl("https://tracking.example/" + request.getOrderId())
                .labelFileUrl("https://label.example/" + request.getOrderId() + ".pdf")
                .issuedAt(issuedAt)
                .build();
        issuedInvoices.put(key(tenantCode, request.getOrderId()), invoice);
        return invoice;
    }

    @Override
    public Map<String, ShipmentInvoiceDto> getShipmentInvoices(String tenantCode, List<String> orderIds) {
        return orderIds.stream()
                .filter(orderId -> issuedInvoices.containsKey(key(tenantCode, orderId)))
                .map(orderId -> Map.entry(orderId, issuedInvoices.get(key(tenantCode, orderId))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void clearIssuedInvoices() {
        issuedInvoices.clear();
    }

    private String key(String tenantCode, String orderId) {
        return tenantCode + "::" + orderId;
    }
}
