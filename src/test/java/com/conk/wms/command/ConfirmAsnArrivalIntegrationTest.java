package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Warehouse;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// 도착 확인 통합 테스트는 HTTP 요청부터 상태 전이/DB 반영까지 실제로 이어지는지 확인한다.
class ConfirmAsnArrivalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository.save(new Warehouse("WH-001", "서울 창고", "TENANT-001"));
        asnRepository.save(new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "REGISTERED",
                "도착 전 ASN",
                2,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 1, 9, 0),
                "SELLER-001",
                "SELLER-001"
        ));
    }

    @Test
    @DisplayName("도착 확인 전체 흐름이 정상 동작하고 ASN 상태가 ARRIVED로 바뀐다")
    void confirmArrival_success() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "arrivedAt", "2026-04-02T10:30:00"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("arrival confirmed"))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.status").value("ARRIVED"))
                .andExpect(jsonPath("$.data.arrivedAt").value("2026-04-02T10:30:00"));

        Optional<Asn> savedAsn = asnRepository.findByAsnId("ASN-001");
        assertThat(savedAsn).isPresent();
        assertThat(savedAsn.get().getStatus()).isEqualTo("ARRIVED");
        assertThat(savedAsn.get().getArrivedAt()).isEqualTo(LocalDateTime.of(2026, 4, 2, 10, 30));
        assertThat(savedAsn.get().getUpdatedBy()).isEqualTo("WH-MANAGER-001");
    }

    @Test
    @DisplayName("도착 확인 실패: 존재하지 않는 ASN이면 404를 반환하고 기존 데이터는 바뀌지 않는다")
    void confirmArrival_whenAsnNotFound_thenReturn404AndKeepDatabase() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-404/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "arrivedAt", "2026-04-02T10:30:00"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-013"));

        Optional<Asn> existingAsn = asnRepository.findByAsnId("ASN-001");
        assertThat(existingAsn).isPresent();
        assertThat(existingAsn.get().getStatus()).isEqualTo("REGISTERED");
        assertThat(existingAsn.get().getArrivedAt()).isNull();
    }

    @Test
    @DisplayName("도착 확인 실패: 이미 ARRIVED 상태면 409를 반환하고 도착 시각을 유지한다")
    void confirmArrival_whenAlreadyArrived_thenReturn409AndKeepArrivalInfo() throws Exception {
        Asn arrivedAsn = new Asn(
                "ASN-ARRIVED-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "ARRIVED",
                "이미 도착한 ASN",
                2,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 10, 30),
                "SELLER-001",
                "WH-MANAGER-001",
                LocalDateTime.of(2026, 4, 2, 10, 30),
                null
        );
        asnRepository.save(arrivedAsn);

        mockMvc.perform(patch("/wms/asns/ASN-ARRIVED-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "arrivedAt", "2026-04-02T11:00:00"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-014"));

        Optional<Asn> savedAsn = asnRepository.findByAsnId("ASN-ARRIVED-001");
        assertThat(savedAsn).isPresent();
        assertThat(savedAsn.get().getStatus()).isEqualTo("ARRIVED");
        assertThat(savedAsn.get().getArrivedAt()).isEqualTo(LocalDateTime.of(2026, 4, 2, 10, 30));
    }
}
