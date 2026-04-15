package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.request.AssignWarehouseManagerRequest;
import com.conk.wms.command.application.dto.request.RegisterWarehouseRequest;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WarehouseManagerAssignment;
import com.conk.wms.command.domain.repository.WarehouseManagerAssignmentRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.service.GetWarehousesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageWarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository;

    @Mock
    private GetWarehousesService getWarehousesService;

    @Mock
    private WarehouseIdGenerator warehouseIdGenerator;

    @InjectMocks
    private ManageWarehouseService manageWarehouseService;

    @Test
    @DisplayName("창고 등록 성공 시 창고와 담당 관리자 배정을 함께 저장한다")
    void register_success() {
        RegisterWarehouseRequest request = request("LA West Coast Hub", "CA", "Los Angeles");

        when(warehouseIdGenerator.generate("CA", "Los Angeles")).thenReturn("WH-LAX-001");
        when(getWarehousesService.getWarehouse("CONK", "WH-LAX-001"))
                .thenReturn(WarehouseResponse.builder().id("WH-LAX-001").name("LA West Coast Hub").build());

        WarehouseResponse response = manageWarehouseService.register("CONK", request);

        assertThat(response.getId()).isEqualTo("WH-LAX-001");
        verify(warehouseRepository).save(any(Warehouse.class));

        ArgumentCaptor<WarehouseManagerAssignment> captor = ArgumentCaptor.forClass(WarehouseManagerAssignment.class);
        verify(warehouseManagerAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getManagerName()).isEqualTo("Michael Jung");
        assertThat(captor.getValue().getManagerEmail()).isEqualTo("m.jung@glsm.com");
    }

    @Test
    @DisplayName("창고 관리자 배정 성공 시 매핑 정보를 갱신한다")
    void assignManager_success() {
        Warehouse warehouse = new Warehouse(
                "WH-001", "CONK", "Main Hub", "서울", "CA", "PST", "Los Angeles", "90001",
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(18, 0), "010-1111-2222", 45000, "ACTIVE", "SYSTEM"
        );
        WarehouseManagerAssignment assignment = new WarehouseManagerAssignment(
                "WH-001", "CONK", "WHM-001", "Old Manager", "old@conk.test", "010-1111-2222", "ACTIVE", null, "SYSTEM"
        );
        AssignWarehouseManagerRequest request = new AssignWarehouseManagerRequestBuilder()
                .managerAccountId("WHM-002")
                .managerName("New Manager")
                .managerEmail("new@conk.test")
                .managerPhone("010-9999-0000")
                .managerStatus("ACTIVE")
                .build();

        when(warehouseRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(warehouse));
        when(warehouseManagerAssignmentRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(assignment));
        when(getWarehousesService.getWarehouse("CONK", "WH-001"))
                .thenReturn(WarehouseResponse.builder().id("WH-001").name("Main Hub").build());

        WarehouseResponse response = manageWarehouseService.assignManager("CONK", "WH-001", request);

        assertThat(response.getId()).isEqualTo("WH-001");
        verify(warehouseManagerAssignmentRepository).save(assignment);
        assertThat(assignment.getManagerName()).isEqualTo("New Manager");
        assertThat(assignment.getManagerEmail()).isEqualTo("new@conk.test");
    }

    private RegisterWarehouseRequest request(String name, String state, String city) {
        return new RegisterWarehouseRequestBuilder()
                .name(name)
                .sqft(45000)
                .address("123 Harbor Blvd")
                .city(city)
                .state(state)
                .openTime("08:00")
                .closeTime("18:00")
                .timezone("PST")
                .managerName("Michael Jung")
                .managerEmail("m.jung@glsm.com")
                .managerPhone("010-1234-5678")
                .build();
    }

    /**
     * 테스트에서만 쓰는 간단한 request 생성기다.
     */
    private static class RegisterWarehouseRequestBuilder {
        private final RegisterWarehouseRequest request = new RegisterWarehouseRequest();

        private RegisterWarehouseRequestBuilder name(String value) { set("name", value); return this; }
        private RegisterWarehouseRequestBuilder sqft(Integer value) { set("sqft", value); return this; }
        private RegisterWarehouseRequestBuilder address(String value) { set("address", value); return this; }
        private RegisterWarehouseRequestBuilder city(String value) { set("city", value); return this; }
        private RegisterWarehouseRequestBuilder state(String value) { set("state", value); return this; }
        private RegisterWarehouseRequestBuilder openTime(String value) { set("openTime", value); return this; }
        private RegisterWarehouseRequestBuilder closeTime(String value) { set("closeTime", value); return this; }
        private RegisterWarehouseRequestBuilder timezone(String value) { set("timezone", value); return this; }
        private RegisterWarehouseRequestBuilder managerName(String value) { set("managerName", value); return this; }
        private RegisterWarehouseRequestBuilder managerEmail(String value) { set("managerEmail", value); return this; }
        private RegisterWarehouseRequestBuilder managerPhone(String value) { set("managerPhone", value); return this; }

        private RegisterWarehouseRequest build() { return request; }

        private void set(String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = RegisterWarehouseRequest.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(request, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class AssignWarehouseManagerRequestBuilder {
        private final AssignWarehouseManagerRequest request = new AssignWarehouseManagerRequest();

        private AssignWarehouseManagerRequestBuilder managerAccountId(String value) { set("managerAccountId", value); return this; }
        private AssignWarehouseManagerRequestBuilder managerName(String value) { set("managerName", value); return this; }
        private AssignWarehouseManagerRequestBuilder managerEmail(String value) { set("managerEmail", value); return this; }
        private AssignWarehouseManagerRequestBuilder managerPhone(String value) { set("managerPhone", value); return this; }
        private AssignWarehouseManagerRequestBuilder managerStatus(String value) { set("managerStatus", value); return this; }

        private AssignWarehouseManagerRequest build() { return request; }

        private void set(String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = AssignWarehouseManagerRequest.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(request, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


