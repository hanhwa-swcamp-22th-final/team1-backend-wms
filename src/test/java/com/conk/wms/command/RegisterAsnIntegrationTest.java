package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

    @BeforeEach
    void setUp() {
        warehouseRepository.save(new Warehouse("WH-001", "서울 창고", "TENANT-001"));
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
}
