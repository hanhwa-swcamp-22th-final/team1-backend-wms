package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderShipmentDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * order/WMS 데이터를 합쳐 integration 송장 payload를 만든다.
 */
@Service
public class ShipmentPayloadResolver {

    private static final String DEFAULT_COUNTRY = "US";

    private final OrderServiceClient orderServiceClient;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public ShipmentPayloadResolver(OrderServiceClient orderServiceClient,
                                   WarehouseRepository warehouseRepository,
                                   ProductRepository productRepository) {
        this.orderServiceClient = orderServiceClient;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    public IssueLabelRequestDto build(String tenantCode,
                                      String orderId,
                                      String carrier,
                                      String service,
                                      String labelFormat) {
        return build(tenantCode, orderId, null, carrier, service, labelFormat);
    }

    public IssueLabelRequestDto build(String tenantCode,
                                      String orderId,
                                      String warehouseId,
                                      String carrier,
                                      String service,
                                      String labelFormat) {
        OrderSummaryDto order = orderServiceClient.getPendingOrder(tenantCode, orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND,
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND.getMessage() + ": " + orderId
                ));
        OrderShipmentDto shipment = orderServiceClient.getOrderShipment(tenantCode, orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND,
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND.getMessage() + ": " + orderId
                ));
        String resolvedWarehouseId = hasText(warehouseId) ? warehouseId : shipment.getWarehouseId();
        Warehouse warehouse = warehouseRepository.findByWarehouseIdAndTenantId(resolvedWarehouseId, tenantCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WAREHOUSE_NOT_FOUND,
                        ErrorCode.WAREHOUSE_NOT_FOUND.getMessage() + ": " + resolvedWarehouseId
                ));

        return IssueLabelRequestDto.builder()
                .orderId(orderId)
                .carrier(carrier)
                .service(service)
                .labelFormat(labelFormat)
                .toAddress(toAddress(shipment))
                .fromAddress(fromAddress(warehouse))
                .parcel(resolveParcel(order))
                .build();
    }

    private IssueLabelRequestDto.AddressDto toAddress(OrderShipmentDto shipment) {
        return IssueLabelRequestDto.AddressDto.builder()
                .name(shipment.getRecipientName())
                .street1(shipment.getStreet1())
                .street2(shipment.getStreet2())
                .city(shipment.getCity())
                .state(shipment.getState())
                .zip(shipment.getZip())
                .country(hasText(shipment.getCountry()) ? shipment.getCountry() : DEFAULT_COUNTRY)
                .phone(shipment.getPhone())
                .email(shipment.getEmail())
                .build();
    }

    private IssueLabelRequestDto.AddressDto fromAddress(Warehouse warehouse) {
        return IssueLabelRequestDto.AddressDto.builder()
                .name(warehouse.getWarehouseName())
                .street1(warehouse.getAddress())
                .city(warehouse.getCityName())
                .state(warehouse.getStateName())
                .zip(warehouse.getZipCode())
                .country(DEFAULT_COUNTRY)
                .phone(warehouse.getPhoneNo())
                .email(null)
                .build();
    }

    private IssueLabelRequestDto.ParcelDto resolveParcel(OrderSummaryDto order) {
        List<String> skuIds = order.getItems().stream()
                .map(OrderItemDto::getSkuId)
                .toList();
        Map<String, Product> productsBySku = productRepository.findAllBySkuIdIn(skuIds).stream()
                .collect(Collectors.toMap(Product::getSkuId, Function.identity()));

        double totalWeight = 0.0d;
        double maxLength = 0.0d;
        double maxWidth = 0.0d;
        double totalHeight = 0.0d;

        for (OrderItemDto item : order.getItems()) {
            Product product = productsBySku.get(item.getSkuId());
            if (product == null) {
                throw new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        ErrorCode.PRODUCT_NOT_FOUND.getMessage() + ": " + item.getSkuId()
                );
            }
            validateParcelSpec(product);
            int quantity = item.getQuantity();
            totalWeight += product.getWeightOz().doubleValue() * quantity;
            maxLength = Math.max(maxLength, product.getDepthIn().doubleValue());
            maxWidth = Math.max(maxWidth, product.getWidthIn().doubleValue());
            totalHeight += product.getHeightIn().doubleValue() * quantity;
        }

        return IssueLabelRequestDto.ParcelDto.builder()
                .weight(round(totalWeight))
                .length(round(maxLength))
                .width(round(maxWidth))
                .height(round(totalHeight))
                .build();
    }

    private void validateParcelSpec(Product product) {
        if (!isPositive(product.getWeightOz())
                || !isPositive(product.getDepthIn())
                || !isPositive(product.getWidthIn())
                || !isPositive(product.getHeightIn())) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_DIMENSION_INVALID,
                    ErrorCode.PRODUCT_DIMENSION_INVALID.getMessage() + ": " + product.getSkuId()
            );
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
