package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.mapper.AsnQueryMapper;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// query 서비스 테스트는 프론트 목록 row shape로 가공되는 값이 맞는지 검증한다.
class GetSellerAsnListServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Spy
    private AsnQueryMapper asnQueryMapper;

    @InjectMocks
    private GetSellerAsnListService getSellerAsnListService;

    @Test
    @DisplayName("셀러 ASN 목록 조회 시 창고명과 품목 집계가 포함된 목록을 반환한다")
    void getSellerAsns_success() {
        Asn firstAsn = createAsn(
                "ASN-20260329-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "REGISTERED",
                "온도 민감 상품 포함"
        );
        Asn secondAsn = createAsn(
                "ASN-20260328-001",
                "WH-002",
                "SELLER-001",
                LocalDate.of(2026, 3, 31),
                LocalDateTime.of(2026, 3, 28, 9, 0),
                "CANCELED",
                "선적 취소"
        );

        when(asnRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(firstAsn, secondAsn));
        when(warehouseRepository.findAllById(List.of("WH-001", "WH-002")))
                .thenReturn(List.of(
                        new Warehouse("WH-001", "NJ Warehouse", "SELLER-001"),
                        new Warehouse("WH-002", "LA Warehouse", "SELLER-001")
                ));
        when(asnItemRepository.findAllByAsnIdIn(List.of("ASN-20260329-001", "ASN-20260328-001")))
                .thenReturn(List.of(
                        new AsnItem("ASN-20260329-001", "SKU-001", 100, "상품A", 3),
                        new AsnItem("ASN-20260329-001", "SKU-002", 50, "상품B", 2),
                        new AsnItem("ASN-20260328-001", "SKU-003", 20, "상품C", 1)
                ));

        List<SellerAsnListItemResponse> result = getSellerAsnListService.getSellerAsns("SELLER-001");

        assertEquals(2, result.size());

        SellerAsnListItemResponse first = result.get(0);
        assertEquals("ASN-20260329-001", first.getId());
        assertEquals("ASN-20260329-001", first.getAsnNo());
        assertEquals("NJ Warehouse", first.getWarehouseName());
        assertEquals("2026-03-30", first.getExpectedDate());
        assertEquals("2026-03-29", first.getCreatedAt());
        assertEquals(2, first.getSkuCount());
        assertEquals(150, first.getTotalQuantity());
        assertEquals("SUBMITTED", first.getStatus());
        assertEquals("REF-29-001", first.getReferenceNo());
        assertEquals("온도 민감 상품 포함", first.getNote());

        SellerAsnListItemResponse second = result.get(1);
        assertEquals("CANCELLED", second.getStatus());
        assertEquals("LA Warehouse", second.getWarehouseName());
    }

    @Test
    @DisplayName("셀러 ASN 이 없으면 빈 목록을 반환한다")
    void getSellerAsns_whenNoData_thenReturnEmpty() {
        when(asnRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of());

        List<SellerAsnListItemResponse> result = getSellerAsnListService.getSellerAsns("SELLER-001");

        assertTrue(result.isEmpty());
    }

    private Asn createAsn(String asnId, String warehouseId, String sellerId, LocalDate expectedDate,
                          LocalDateTime createdAt, String status, String sellerMemo) {
        return new Asn(
                asnId,
                warehouseId,
                sellerId,
                expectedDate,
                status,
                sellerMemo,
                5,
                createdAt,
                createdAt,
                sellerId,
                sellerId
        );
    }
}
