package com.conk.wms.query.mapper;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.query.controller.dto.response.AsnDetailResponse;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.controller.dto.response.AsnKpiResponse;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// query mapper 테스트는 엔티티/집계값이 화면 응답 shape로 올바르게 변환되는지 검증한다.
class AsnQueryMapperTest {

    private final AsnQueryMapper asnQueryMapper = new AsnQueryMapper();

    @Test
    @DisplayName("Seller ASN 목록 row를 화면 응답 형태로 변환한다")
    void toSellerAsnListItemResponse_success() {
        Asn asn = createAsn("ASN-20260329-001", "REGISTERED");
        List<AsnItem> items = List.of(
                new AsnItem("ASN-20260329-001", "SKU-001", 100, "상품A", 3),
                new AsnItem("ASN-20260329-001", "SKU-002", 50, "상품B", 2)
        );

        SellerAsnListItemResponse result =
                asnQueryMapper.toSellerAsnListItemResponse(asn, items, "NJ Warehouse");

        assertEquals("ASN-20260329-001", result.getId());
        assertEquals("ASN-20260329-001", result.getAsnNo());
        assertEquals("NJ Warehouse", result.getWarehouseName());
        assertEquals("2026-03-30", result.getExpectedDate());
        assertEquals("2026-03-29", result.getCreatedAt());
        assertEquals(2, result.getSkuCount());
        assertEquals(150, result.getTotalQuantity());
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals("REF-29-001", result.getReferenceNo());
        assertEquals("메모", result.getNote());
    }

    @Test
    @DisplayName("ASN 상세 응답으로 변환할 때 품목/집계/detail 블록을 함께 채운다")
    void toAsnDetailResponse_success() {
        Asn asn = createAsn("ASN-20260329-001", "STORED");
        List<AsnItem> items = List.of(
                new AsnItem("ASN-20260329-001", "SKU-001", 100, "루미에르 앰플 30ml", 3),
                new AsnItem("ASN-20260329-001", "SKU-002", 50, "리페어 마스크팩 10입", 2)
        );

        AsnDetailResponse result =
                asnQueryMapper.toAsnDetailResponse(asn, items, "서울 창고");

        assertEquals("ASN-20260329-001", result.getId());
        assertEquals("RECEIVED", result.getStatus());
        assertEquals("서울 창고", result.getWarehouseName());
        assertEquals(2, result.getSkuCount());
        assertEquals(150, result.getTotalQuantity());
        assertEquals("SKU-001, SKU-002", result.getSku());
        assertEquals("REF-29-001", result.getReferenceNo());
        assertEquals(5, result.getDetail().getTotalCartons());
        assertEquals(2, result.getDetail().getItems().size());
        assertEquals("Packing List", result.getDetail().getDocuments().get(0));
    }

    @Test
    @DisplayName("KPI 집계 시 seller 화면 상태 규칙으로 total/submitted/received/cancelled를 계산한다")
    void toAsnKpiResponse_success() {
        List<Asn> asns = List.of(
                createAsn("ASN-001", "REGISTERED"),
                createAsn("ASN-002", "ARRIVED"),
                createAsn("ASN-003", "STORED"),
                createAsn("ASN-004", "CANCELED")
        );

        AsnKpiResponse result = asnQueryMapper.toAsnKpiResponse(asns);

        assertEquals(4, result.getTotal());
        assertEquals(1, result.getSubmitted());
        assertEquals(2, result.getReceived());
        assertEquals(1, result.getCancelled());
    }

    @Test
    @DisplayName("ASN 검수/적재 응답으로 변환할 때 원본 품목과 저장된 inspection 결과를 합친다")
    void toAsnInspectionResponse_success() {
        Asn asn = createAsn("ASN-001", "INSPECTING_PUTAWAY");
        List<AsnItem> items = List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3),
                new AsnItem("ASN-001", "SKU-002", 50, "상품B", 2)
        );
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.saveProgress("LOC-A-01-01", 100, 3, "박스 파손", 97);

        AsnInspectionResponse result = asnQueryMapper.toAsnInspectionResponse(asn, items, List.of(row));

        assertEquals("ASN-001", result.getAsnId());
        assertEquals("INSPECTING_PUTAWAY", result.getStatus());
        assertEquals(2, result.getItems().size());
        assertEquals("SKU-001", result.getItems().get(0).getSkuId());
        assertEquals(100, result.getItems().get(0).getInspectedQuantity());
        assertEquals(97, result.getItems().get(0).getPutawayQuantity());
        assertEquals("LOC-A-01-01", result.getItems().get(0).getLocationId());
        assertEquals(0, result.getItems().get(1).getInspectedQuantity());
        assertEquals(0, result.getItems().get(1).getPutawayQuantity());
    }

    private Asn createAsn(String asnId, String status) {
        return new Asn(
                asnId,
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                status,
                "메모",
                5,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "SELLER-001",
                "SELLER-001"
        );
    }
}
