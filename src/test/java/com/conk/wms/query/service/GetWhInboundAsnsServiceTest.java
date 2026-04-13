package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WhManagerInboundAsnResponse;
import com.conk.wms.query.mapper.AsnQueryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetWhInboundAsnsServiceTest {

    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final AsnRepository asnRepository = mock(AsnRepository.class);
    private final AsnItemRepository asnItemRepository = mock(AsnItemRepository.class);
    private final InspectionPutawayRepository inspectionPutawayRepository = mock(InspectionPutawayRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final GetWhInboundAsnsService service = new GetWhInboundAsnsService(
            warehouseRepository,
            asnRepository,
            asnItemRepository,
            inspectionPutawayRepository,
            productRepository,
            new AsnQueryMapper()
    );

    @Test
    @DisplayName("창고 관리자 ASN 목록 조회 시 tenant 창고 기준 ASN 목록을 화면용 응답으로 조합한다")
    void getInboundAsns_success() {
        Asn first = createAsn("ASN-002", "REGISTERED", LocalDateTime.of(2026, 4, 13, 10, 0));
        Asn second = createAsn("ASN-001", "STORED", LocalDateTime.of(2026, 4, 12, 9, 0));
        List<AsnItem> items = List.of(
                new AsnItem("ASN-002", "SKU-NEW", 20, "신규 상품", 1),
                new AsnItem("ASN-001", "SKU-OLD", 30, "기존 상품", 2)
        );
        InspectionPutaway inspection = new InspectionPutaway("ASN-001", "SKU-OLD", "CONK");
        inspection.saveProgress("LOC-A-01-01", 30, 0, null, 30);
        inspection.complete();

        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK"))
                .thenReturn(List.of(new Warehouse("WH-001", "서울 창고", "CONK")));
        when(asnRepository.findAllByWarehouseIdIn(List.of("WH-001")))
                .thenReturn(List.of(second, first));
        when(asnItemRepository.findAllByAsnIdIn(List.of("ASN-002", "ASN-001")))
                .thenReturn(items);
        when(inspectionPutawayRepository.findAllByAsnIdIn(List.of("ASN-002", "ASN-001")))
                .thenReturn(List.of(inspection));
        when(productRepository.findAllBySkuIdIn(anyCollection()))
                .thenReturn(List.of(
                        createProduct("SKU-NEW", "신규 상품"),
                        createProduct("SKU-OLD", "기존 상품")
                ));
        when(inspectionPutawayRepository
                .findAllBySkuIdInAndTenantIdAndCompletedTrueAndLocationIdIsNotNullOrderByCompletedAtDescUpdatedAtDesc(
                        anyCollection(), eq("CONK")))
                .thenReturn(List.of(inspection));

        List<WhManagerInboundAsnResponse> result = service.getInboundAsns("CONK");

        assertEquals(2, result.size());
        assertEquals("ASN-002", result.get(0).getId());
        assertEquals("PENDING", result.get(0).getStatus());
        assertNull(result.get(0).getActualQty());
        assertEquals(1, result.get(0).getNewSkus().size());
        assertEquals("SKU-NEW", result.get(0).getNewSkus().get(0).getCode());

        assertEquals("ASN-001", result.get(1).getId());
        assertEquals("RECEIVED", result.get(1).getStatus());
        assertEquals(30, result.get(1).getActualQty());
        assertEquals(0, result.get(1).getNewSkus().size());
    }

    private Asn createAsn(String asnId, String status, LocalDateTime createdAt) {
        return new Asn(
                asnId,
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 14),
                status,
                "메모",
                3,
                createdAt,
                createdAt,
                "SELLER-001",
                "SELLER-001"
        );
    }

    private Product createProduct(String skuId, String productName) {
        return new Product(
                skuId,
                productName,
                "뷰티",
                10000,
                7000,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                10,
                "ACTIVE",
                "SELLER-001",
                "SELLER-001"
        );
    }
}
