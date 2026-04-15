package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.MasterAsnListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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

        when(asnRepository.findAll(any(Sort.class))).thenReturn(List.of(latest, earlier));
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

        List<MasterAsnListItemResponse> responses = getMasterAsnListService.getAsns(null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("ASN-002");
        assertThat(responses.get(0).getWarehouse()).isEqualTo("Sub Hub");
        assertThat(responses.get(0).getStatus()).isEqualTo("RECEIVED");
        assertThat(responses.get(1).getId()).isEqualTo("ASN-001");
        assertThat(responses.get(1).getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("RECEIVED 필터는 도착/검수/보관 완료 상태를 함께 조회한다")
    void getAsns_filtersByNormalizedStatusInRepository() {
        when(asnRepository.findAllByStatusInOrderByCreatedAtDesc(anyCollection()))
                .thenReturn(List.of(asn("ASN-010", "WH-001", "STORED", LocalDateTime.of(2026, 4, 12, 10, 0))));
        when(warehouseRepository.findAllById(anyCollection()))
                .thenReturn(List.of(warehouse("WH-001", "CONK", "Main Hub")));
        when(asnItemRepository.findAllByAsnIdIn(any()))
                .thenReturn(List.of(new AsnItem("ASN-010", "SKU-010", 5, "X", 2)));

        List<MasterAsnListItemResponse> responses = getMasterAsnListService.getAsns("RECEIVED");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("RECEIVED");
        assertThat(responses.get(0).getPlannedQty()).isEqualTo(5);
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
