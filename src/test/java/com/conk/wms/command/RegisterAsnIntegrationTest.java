package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// 통합 테스트는 HTTP -> 서비스 -> DB 저장까지 한 번에 확인한다.
class RegisterAsnIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private AsnItemRepository asnItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private SellerWarehouseRepository sellerWarehouseRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository.save(new Warehouse("WH-001", "서울 창고", "TENANT-001"));
        sellerWarehouseRepository.save(new SellerWarehouse("SELLER-001", "WH-001", true, "SYSTEM"));
    }

    @Test
    @DisplayName("Seller ASN 등록 전체 흐름이 정상 동작하고 DB에 저장된다")
    void registerAsn_success() throws Exception {
        Map<String, Object> body = Map.of(
                "asnNo", "ASN-20260329-001",
                "warehouseId", "WH-001",
                "expectedDate", "2026-03-29",
                "note", "온도 민감 상품 포함",
                "originCountry", "KR",
                "senderAddress", "서울시 강남구 테헤란로 123",
                "senderPhone", "010-1234-5678",
                "shippingMethod", "AIR",
                "detail", Map.of(
                        "items", List.of(
                                Map.of(
                                        "sku", "SKU-001",
                                        "productName", "루미에르 앰플 30ml",
                                        "quantity", 100,
                                        "cartons", 5
                                )
                        )
                )
        );

        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ASN-20260329-001"));

        Optional<Asn> savedAsn = asnRepository.findByAsnId("ASN-20260329-001");
        List<AsnItem> savedItems = asnItemRepository.findAllByAsnId("ASN-20260329-001");

        assertThat(savedAsn).isPresent();
        assertThat(savedAsn.get().getWarehouseId()).isEqualTo("WH-001");
        assertThat(savedAsn.get().getSellerId()).isEqualTo("SELLER-001");
        assertThat(savedAsn.get().getExpectedArrivalDate()).isEqualTo(LocalDate.of(2026, 3, 29));
        assertThat(savedAsn.get().getStatus()).isEqualTo("REGISTERED");
        assertThat(savedAsn.get().getSellerMemo()).isEqualTo("온도 민감 상품 포함");
        assertThat(savedAsn.get().getBoxQuantity()).isEqualTo(5);

        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getSkuId()).isEqualTo("SKU-001");
        assertThat(savedItems.get(0).getProductNameSnapshot()).isEqualTo("루미에르 앰플 30ml");
        assertThat(savedItems.get(0).getQuantity()).isEqualTo(100);
        assertThat(savedItems.get(0).getBoxQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("셀러 ASN 목록 조회 시 본인 ASN 목록과 집계 정보가 반환된다")
    void getSellerAsns_success() throws Exception {
        asnRepository.save(new Asn(
                "ASN-20260329-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                "REGISTERED",
                "온도 민감 상품 포함",
                5,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "SELLER-001",
                "SELLER-001"
        ));
        asnRepository.save(new Asn(
                "ASN-20260328-001",
                "WH-001",
                "SELLER-002",
                LocalDate.of(2026, 3, 31),
                "REGISTERED",
                "다른 셀러 데이터",
                1,
                LocalDateTime.of(2026, 3, 28, 10, 0),
                LocalDateTime.of(2026, 3, 28, 10, 0),
                "SELLER-002",
                "SELLER-002"
        ));
        asnItemRepository.save(new AsnItem("ASN-20260329-001", "SKU-001", 100, "루미에르 앰플 30ml", 3));
        asnItemRepository.save(new AsnItem("ASN-20260329-001", "SKU-002", 50, "리페어 마스크팩 10입", 2));
        asnItemRepository.save(new AsnItem("ASN-20260328-001", "SKU-003", 20, "다른 셀러 상품", 1));

        mockMvc.perform(get("/wms/seller/asns")
                        .param("page", "0")
                        .param("size", "20")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("ASN-20260329-001"))
                .andExpect(jsonPath("$.data.items[0].asnNo").value("ASN-20260329-001"))
                .andExpect(jsonPath("$.data.items[0].warehouseName").value("서울 창고"))
                .andExpect(jsonPath("$.data.items[0].skuCount").value(2))
                .andExpect(jsonPath("$.data.items[0].totalQuantity").value(150))
                .andExpect(jsonPath("$.data.items[0].status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.items[0].referenceNo").isEmpty())
                .andExpect(jsonPath("$.data.items[0].note").value("온도 민감 상품 포함"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @DisplayName("ASN 상세 조회 시 기본 정보와 품목 상세가 함께 반환된다")
    void getAsnDetail_success() throws Exception {
        asnRepository.save(new Asn(
                "ASN-20260329-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                "REGISTERED",
                "온도 민감 상품 포함",
                5,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "SELLER-001",
                "SELLER-001"
        ));
        asnItemRepository.save(new AsnItem("ASN-20260329-001", "SKU-001", 100, "루미에르 앰플 30ml", 3));
        asnItemRepository.save(new AsnItem("ASN-20260329-001", "SKU-002", 50, "리페어 마스크팩 10입", 2));

        mockMvc.perform(get("/wms/asns/ASN-20260329-001")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ASN-20260329-001"))
                .andExpect(jsonPath("$.data.warehouse").value("서울 창고"))
                .andExpect(jsonPath("$.data.skuCount").value(2))
                .andExpect(jsonPath("$.data.totalQuantity").value(150))
                .andExpect(jsonPath("$.data.detail.totalCartons").value(5))
                .andExpect(jsonPath("$.data.detail.items.length()").value(2))
                .andExpect(jsonPath("$.data.detail.items[0].sku").value("SKU-001"));
    }

    @Test
    @DisplayName("ASN KPI 조회 시 본인 ASN 상태별 건수가 반환된다")
    void getAsnKpi_success() throws Exception {
        asnRepository.save(new Asn(
                "ASN-20260329-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                "REGISTERED",
                "제출됨 데이터",
                1,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "SELLER-001",
                "SELLER-001"
        ));
        asnRepository.save(new Asn(
                "ASN-20260329-002",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                "ARRIVED",
                "입고완료 데이터",
                1,
                LocalDateTime.of(2026, 3, 29, 11, 0),
                LocalDateTime.of(2026, 3, 29, 11, 0),
                "SELLER-001",
                "SELLER-001"
        ));
        asnRepository.save(new Asn(
                "ASN-20260329-003",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                "CANCELED",
                "취소 데이터",
                1,
                LocalDateTime.of(2026, 3, 29, 12, 0),
                LocalDateTime.of(2026, 3, 29, 12, 0),
                "SELLER-001",
                "SELLER-001"
        ));
        asnRepository.save(new Asn(
                "ASN-20260329-004",
                "WH-001",
                "SELLER-002",
                LocalDate.of(2026, 3, 30),
                "REGISTERED",
                "다른 셀러 데이터",
                1,
                LocalDateTime.of(2026, 3, 29, 13, 0),
                LocalDateTime.of(2026, 3, 29, 13, 0),
                "SELLER-002",
                "SELLER-002"
        ));

        mockMvc.perform(get("/wms/asns/kpi")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.submitted").value(1))
                .andExpect(jsonPath("$.data.received").value(1))
                .andExpect(jsonPath("$.data.cancelled").value(1));
    }

    @Test
    @DisplayName("존재하지 않는 창고면 400을 반환하고 DB에 저장되지 않는다")
    void registerAsn_whenWarehouseNotFound_thenReturn400() throws Exception {
        Map<String, Object> body = Map.of(
                "asnNo", "ASN-20260329-001",
                "warehouseId", "WH-999",
                "expectedDate", "2026-03-29",
                "note", "메모",
                "detail", Map.of(
                        "items", List.of(
                                Map.of(
                                        "sku", "SKU-001",
                                        "productName", "루미에르 앰플 30ml",
                                        "quantity", 100,
                                        "cartons", 5
                                )
                        )
                )
        );

        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-010"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 창고입니다: WH-999"));

        assertThat(asnRepository.existsByAsnId("ASN-20260329-001")).isFalse();
    }

    @Test
    @DisplayName("셀러와 연결되지 않은 창고면 400을 반환한다")
    void registerAsn_whenSellerWarehouseMismatch_thenReturn400() throws Exception {
        warehouseRepository.save(new Warehouse("WH-002", "부산 창고", "TENANT-001"));

        Map<String, Object> body = Map.of(
                "asnNo", "ASN-20260329-009",
                "warehouseId", "WH-002",
                "expectedDate", "2026-03-29",
                "note", "메모",
                "detail", Map.of(
                        "items", List.of(
                                Map.of(
                                        "sku", "SKU-001",
                                        "productName", "루미에르 앰플 30ml",
                                        "quantity", 100,
                                        "cartons", 5
                                )
                        )
                )
        );

        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-037"));

        assertThat(asnRepository.existsByAsnId("ASN-20260329-009")).isFalse();
    }
}
