package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.MasterAsnListItemResponse;
import com.conk.wms.query.controller.dto.response.MasterAsnListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMasterAsnListServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private GetMasterAsnListService getMasterAsnListService;

    @Test
    @DisplayName("상태 필터가 없으면 생성일 내림차순 ASN 목록을 그대로 조합한다")
    void getAsns_returnsSortedResultsWhenStatusIsBlank() {
        Asn latest = asn("ASN-002", "WH-002", "ARRIVED", LocalDateTime.of(2026, 4, 12, 9, 0));
        Asn earlier = asn("ASN-001", "WH-001", "REGISTERED", LocalDateTime.of(2026, 4, 10, 9, 0));

        when(asnRepository.findMasterAsns(anyCollection(), eq(true), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(latest, earlier), PageRequest.of(0, 10), 2));
        when(asnRepository.count()).thenReturn(2L);
        when(asnRepository.countByStatusIn(List.of("REGISTERED"))).thenReturn(1L);
        when(asnRepository.countByStatusIn(List.of("ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED"))).thenReturn(1L);
        when(asnRepository.countByStatusIn(List.of("CANCELED", "CANCELLED"))).thenReturn(0L);
        when(asnRepository.findDistinctWarehouseIds()).thenReturn(List.of("WH-001", "WH-002"));
        when(asnRepository.findDistinctSellerIds()).thenReturn(List.of("SELLER-001"));
        when(warehouseRepository.findAllById(anyCollection()))
                .thenReturn(List.of(
                        warehouse("WH-001", "CONK", "Main Hub"),
                        warehouse("WH-002", "CONK", "Sub Hub")
                ));
        when(asnItemRepository.findAllByAsnIdIn(any()))
                .thenReturn(List.of(
                        new AsnItem("ASN-001", "SKU-001", 2, "A", 1),
                        new AsnItem("ASN-002", "SKU-002", 3, "B", 1)
                ));

        MasterAsnListResponse response = getMasterAsnListService.getAsns(null, null, null, null, 1, 10);
        List<MasterAsnListItemResponse> responses = response.getItems();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("ASN-002");
        assertThat(responses.get(0).getWarehouse()).isEqualTo("Sub Hub");
        assertThat(responses.get(0).getStatus()).isEqualTo("RECEIVED");
        assertThat(responses.get(1).getId()).isEqualTo("ASN-001");
        assertThat(responses.get(1).getStatus()).isEqualTo("SUBMITTED");
        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getCounts()).containsEntry("ALL", 2L).containsEntry("SUBMITTED", 1L);
        assertThat(response.getWarehouseOptions()).hasSize(2);
        assertThat(response.getCompanyOptions()).extracting(MasterAsnListResponse.OptionResponse::getValue)
                .containsExactly("SELLER-001");
    }

    @Test
    @DisplayName("RECEIVED 필터는 도착/검수/보관 완료 상태를 함께 조회한다")
    void getAsns_filtersByNormalizedStatusInRepository() {
        when(asnRepository.findMasterAsns(anyCollection(), eq(false), eq("WH-001"), eq("SELLER-001"), eq("ASN-010"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(
                        List.of(asn("ASN-010", "WH-001", "STORED", LocalDateTime.of(2026, 4, 12, 10, 0))),
                        PageRequest.of(1, 20),
                        1
                ));
        when(asnRepository.count()).thenReturn(10L);
        when(asnRepository.countByStatusIn(List.of("REGISTERED"))).thenReturn(3L);
        when(asnRepository.countByStatusIn(List.of("ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED"))).thenReturn(5L);
        when(asnRepository.countByStatusIn(List.of("CANCELED", "CANCELLED"))).thenReturn(2L);
        when(asnRepository.findDistinctWarehouseIds()).thenReturn(List.of("WH-001"));
        when(asnRepository.findDistinctSellerIds()).thenReturn(List.of("SELLER-001", "SELLER-002"));
        when(warehouseRepository.findAllById(anyCollection()))
                .thenReturn(List.of(warehouse("WH-001", "CONK", "Main Hub")));
        when(asnItemRepository.findAllByAsnIdIn(any()))
                .thenReturn(List.of(new AsnItem("ASN-010", "SKU-010", 5, "X", 2)));

        MasterAsnListResponse response = getMasterAsnListService.getAsns("RECEIVED", "WH-001", "SELLER-001", "ASN-010", 2, 20);
        List<MasterAsnListItemResponse> responses = response.getItems();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("RECEIVED");
        assertThat(responses.get(0).getPlannedQty()).isEqualTo(5);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getCounts()).isEqualTo(Map.of(
                "ALL", 10L,
                "SUBMITTED", 3L,
                "RECEIVED", 5L,
                "CANCELLED", 2L
        ));
    }

    private Warehouse warehouse(String warehouseId, String tenantId, String name) {
        return new Warehouse(
                warehouseId,
                tenantId,
                name,
                "address",
                "state",
                "KST",
                "city",
                "00000",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "010",
                1000,
                "ACTIVE",
                "SYSTEM"
        );
    }

    private Asn asn(String asnId, String warehouseId, String status, LocalDateTime createdAt) {
        return new Asn(
                asnId,
                warehouseId,
                "SELLER-001",
                LocalDate.of(2026, 4, 10),
                status,
                null,
                1,
                createdAt,
                createdAt,
                "SYSTEM",
                "SYSTEM"
        );
    }
}
