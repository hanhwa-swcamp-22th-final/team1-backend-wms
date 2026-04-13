package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.query.controller.dto.response.BinCountSummaryResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBillingBinCountSummariesServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private GetBillingBinCountSummariesService getBillingBinCountSummariesService;

    @Test
    @DisplayName("seller와 warehouse별로 quantity가 0보다 큰 distinct bin 수를 계산한다")
    void getBinCountSummaries_success() {
        when(inventoryRepository.findAllByQuantityGreaterThan(0)).thenReturn(List.of(
                new Inventory("LOC-001", "SKU-001", "SELLER-001", 10, "AVAILABLE"),
                new Inventory("LOC-001", "SKU-002", "SELLER-001", 2, "ALLOCATED"),
                new Inventory("LOC-002", "SKU-003", "SELLER-001", 5, "AVAILABLE"),
                new Inventory("LOC-003", "SKU-004", "SELLER-002", 1, "AVAILABLE")
        ));
        when(locationRepository.findAllByLocationIdIn(Set.of("LOC-001", "LOC-002", "LOC-003"))).thenReturn(List.of(
                new Location("LOC-001", "BIN-001", "WH-001", "ZONE-A", "RACK-1", 100, true),
                new Location("LOC-002", "BIN-002", "WH-001", "ZONE-A", "RACK-1", 100, true),
                new Location("LOC-003", "BIN-003", "WH-002", "ZONE-B", "RACK-2", 100, true)
        ));

        List<BinCountSummaryResponse> result =
                getBillingBinCountSummariesService.getBinCountSummaries(LocalDate.of(2026, 4, 13));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSellerId()).isEqualTo("SELLER-001");
        assertThat(result.get(0).getWarehouseId()).isEqualTo("WH-001");
        assertThat(result.get(0).getOccupiedBinCount()).isEqualTo(2);
        assertThat(result.get(1).getSellerId()).isEqualTo("SELLER-002");
        assertThat(result.get(1).getOccupiedBinCount()).isEqualTo(1);
    }
}
