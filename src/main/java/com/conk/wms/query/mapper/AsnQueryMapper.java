package com.conk.wms.query.mapper;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.query.controller.dto.response.AsnDetailResponse;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.controller.dto.response.AsnKpiResponse;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ASN 조회 전용 read model을 SQL로 조회하는 매퍼다.
 */
@Component
// ASN query 응답 가공을 한 곳에 모아두는 mapper.
// 서비스는 조회/조합에 집중하고, 화면용 shape 변환 책임은 이 클래스로 분리한다.
public class AsnQueryMapper {

    // Seller ASN 목록 한 row를 프론트가 기대하는 응답 shape로 변환한다.
    public SellerAsnListItemResponse toSellerAsnListItemResponse(Asn asn, List<AsnItem> items, String warehouseName) {
        int skuCount = (int) items.stream()
                .map(AsnItem::getSkuId)
                .distinct()
                .count();
        int totalQuantity = items.stream()
                .mapToInt(AsnItem::getQuantity)
                .sum();

        return SellerAsnListItemResponse.builder()
                .id(asn.getAsnId())
                .asnNo(asn.getAsnId())
                .warehouseName(warehouseName)
                .expectedDate(asn.getExpectedArrivalDate().toString())
                .createdAt(asn.getCreatedAt().toLocalDate().toString())
                .skuCount(skuCount)
                .totalQuantity(totalQuantity)
                .status(toSellerStatus(asn.getStatus()))
                .referenceNo(buildReferenceNo(asn.getAsnId()))
                .note(asn.getSellerMemo())
                .build();
    }

    // ASN 헤더와 품목 목록을 seller 상세 모달이 바로 쓸 수 있는 응답으로 변환한다.
    public AsnDetailResponse toAsnDetailResponse(Asn asn, List<AsnItem> items, String warehouseName) {
        int totalQuantity = items.stream()
                .mapToInt(AsnItem::getQuantity)
                .sum();
        int totalCartons = items.stream()
                .mapToInt(AsnItem::getBoxQuantity)
                .sum();
        String skuSummary = items.stream()
                .map(AsnItem::getSkuId)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");

        List<AsnDetailResponse.ItemResponse> itemResponses = items.stream()
                .map(item -> AsnDetailResponse.ItemResponse.builder()
                        .sku(item.getSkuId())
                        .productName(item.getProductNameSnapshot())
                        .quantity(item.getQuantity())
                        .cartons(item.getBoxQuantity())
                        .build())
                .toList();

        return AsnDetailResponse.builder()
                .id(asn.getAsnId())
                .asnNo(asn.getAsnId())
                .status(toSellerStatus(asn.getStatus()))
                .company(asn.getSellerId())
                .seller(asn.getSellerId())
                .warehouse(warehouseName)
                .warehouseName(warehouseName)
                .skuCount((int) items.stream().map(AsnItem::getSkuId).distinct().count())
                .totalQuantity(totalQuantity)
                .plannedQty(totalQuantity)
                .actualQty(null)
                .sku(skuSummary)
                .expectedDate(asn.getExpectedArrivalDate().toString())
                .createdAt(asn.getCreatedAt().toLocalDate().toString())
                .registeredDate(asn.getCreatedAt().toLocalDate().toString())
                .referenceNo(buildReferenceNo(asn.getAsnId()))
                .note(asn.getSellerMemo())
                .detail(AsnDetailResponse.DetailResponse.builder()
                        .supplierName(asn.getSellerId())
                        // ERD에 운송/서류 컬럼이 아직 없어, 상세 화면이 깨지지 않도록 기본값을 함께 구성한다.
                        .originCountry("정보 없음")
                        .originPort("-")
                        .transportMode("-")
                        .incoterms("-")
                        .bookingNo(buildReferenceNo(asn.getAsnId()))
                        .carrier("정보 없음")
                        .arrivalWindow(asn.getExpectedArrivalDate().toString())
                        .documents(List.of("Packing List"))
                        .totalCartons(totalCartons)
                        .items(itemResponses)
                        .build())
                .build();
    }

    // KPI는 seller 화면과 같은 상태 규칙으로 전체 ASN 목록을 집계해 계산한다.
    public AsnKpiResponse toAsnKpiResponse(List<Asn> asns) {
        int submitted = 0;
        int received = 0;
        int cancelled = 0;

        for (Asn asn : asns) {
            switch (toSellerStatus(asn.getStatus())) {
                case "SUBMITTED" -> submitted++;
                case "RECEIVED" -> received++;
                case "CANCELLED" -> cancelled++;
                default -> {
                    // seller 화면에서 정의하지 않은 상태는 total에만 포함한다.
                }
            }
        }

        return AsnKpiResponse.builder()
                .total(asns.size())
                .submitted(submitted)
                .received(received)
                .cancelled(cancelled)
                .build();
    }

    // 검수/적재 화면용 응답으로 ASN 품목과 inspection_putaway 저장값을 합친다.
    public AsnInspectionResponse toAsnInspectionResponse(Asn asn, List<AsnItem> items,
                                                         List<InspectionPutaway> inspectionRows) {
        Map<String, InspectionPutaway> rowBySkuId = inspectionRows.stream()
                .collect(Collectors.toMap(InspectionPutaway::getSkuId, Function.identity()));

        List<AsnInspectionResponse.ItemResponse> itemResponses = items.stream()
                .map(item -> {
                    InspectionPutaway row = rowBySkuId.get(item.getSkuId());
                    return AsnInspectionResponse.ItemResponse.builder()
                            .skuId(item.getSkuId())
                            .productName(item.getProductNameSnapshot())
                            .plannedQuantity(item.getQuantity())
                            .boxQuantity(item.getBoxQuantity())
                            .inspectedQuantity(row != null ? row.getInspectedQuantity() : 0)
                            .defectiveQuantity(row != null ? row.getDefectiveQuantity() : 0)
                            .defectReason(row != null ? row.getDefectReason() : null)
                            .locationId(row != null ? row.getLocationId() : null)
                            .putawayQuantity(row != null ? row.getPutawayQuantity() : 0)
                            .completed(row != null && row.isCompleted())
                            .startedAt(row != null ? row.getStartedAt() : null)
                            .completedAt(row != null ? row.getCompletedAt() : null)
                            .build();
                })
                .toList();

        return AsnInspectionResponse.builder()
                .asnId(asn.getAsnId())
                .status(asn.getStatus())
                .items(itemResponses)
                .build();
    }

    // DB 운영 상태를 seller 화면에서 사용하는 상태 배지 값으로 맞춘다.
    private String toSellerStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SUBMITTED";
        }
        return switch (status) {
            case "REGISTERED" -> "SUBMITTED";
            case "ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED" -> "RECEIVED";
            case "CANCELED", "CANCELLED" -> "CANCELLED";
            default -> status;
        };
    }

    // referenceNo 원본 컬럼이 아직 없어 ASN 번호 기반 임시 가공값을 내려준다.
    private String buildReferenceNo(String asnId) {
        if (asnId == null || asnId.isBlank()) {
            return "REF-PENDING";
        }
        int startIndex = Math.max(0, asnId.length() - 6);
        return "REF-" + asnId.substring(startIndex);
    }
}
