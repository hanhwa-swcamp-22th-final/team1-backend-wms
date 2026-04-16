package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationQuantityProjection;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.query.controller.dto.response.LocationZoneResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLocationsServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private GetLocationsService getLocationsService;

    @Test
    @DisplayName("location 응답은 location별 집계 재고 수량을 사용해 상태를 계산한다")
    void getLocations_usesAggregatedQuantityByLocation() {
        when(inventoryRepository.sumQuantityByLocation("CONK"))
                .thenReturn(List.of(
                        locationQuantity("LOC-A-01", 80),
                        locationQuantity("LOC-A-02", 0)
                ));
        when(locationRepository.findAllByActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc())
                .thenReturn(List.of(
                        new Location("LOC-A-01", "A-01", "WH-001", "A", "R1", 100, true),
                        new Location("LOC-A-02", "A-02", "WH-001", "A", "R1", 100, true)
                ));

        List<LocationZoneResponse> response = getLocationsService.getLocations("CONK");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRacks()).hasSize(1);
        assertThat(response.get(0).getRacks().get(0).getBins()).hasSize(2);
        assertThat(response.get(0).getRacks().get(0).getBins().get(0).getUsedQty()).isEqualTo(80);
        assertThat(response.get(0).getRacks().get(0).getBins().get(0).getStatus()).isEqualTo("caution");
        assertThat(response.get(0).getRacks().get(0).getBins().get(1).getStatus()).isEqualTo("empty");
    }

    private LocationQuantityProjection locationQuantity(String locationId, int usedQuantity) {
        return new LocationQuantityProjection() {
            @Override
            public String getLocationId() {
                return locationId;
            }

            @Override
            public Integer getUsedQuantity() {
                return usedQuantity;
            }
        };
    }
}
