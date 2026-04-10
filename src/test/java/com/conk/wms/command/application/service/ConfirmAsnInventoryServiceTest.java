package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.InventoryId;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.application.dto.ConfirmAsnInventoryCommand;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmAsnInventoryServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ConfirmAsnInventoryService confirmAsnInventoryService;

    @Test
    @DisplayName("입고 확정 성공: 완료된 적재 수량을 AVAILABLE 재고로 반영하고 ASN을 STORED로 변경한다")
    void confirm_success() {
        Asn asn = createAsn("INSPECTING_PUTAWAY");
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.saveProgress("LOC-A-01-01", 100, 3, "파손", 97);
        row.complete();

        Inventory existingInventory = new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE");

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(row));
        when(inventoryRepository.findById(new InventoryId("LOC-A-01-01", "SKU-001", "CONK", "AVAILABLE")))
                .thenReturn(Optional.of(existingInventory));
        when(asnRepository.save(any(Asn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmAsnInventoryService.ConfirmResult result = confirmAsnInventoryService.confirm(
                new ConfirmAsnInventoryCommand("ASN-001", "CONK")
        );

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());

        assertEquals("STORED", result.getAsn().getStatus());
        assertEquals(1, result.getReflectedInventoryCount());
        assertEquals(107, inventoryCaptor.getValue().getQuantity());
        assertEquals("AVAILABLE", inventoryCaptor.getValue().getType());
    }

    @Test
    @DisplayName("입고 확정 실패: inspection row가 미완료 상태면 예외가 발생한다")
    void confirm_whenInspectionNotCompleted_thenThrow() {
        Asn asn = createAsn("INSPECTING_PUTAWAY");
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.saveProgress("LOC-A-01-01", 100, 0, null, 100);

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(row));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> confirmAsnInventoryService.confirm(new ConfirmAsnInventoryCommand("ASN-001", "CONK")));

        assertEquals(ErrorCode.ASN_CONFIRM_INCOMPLETE, exception.getErrorCode());
        verify(inventoryRepository, times(0)).save(any());
    }

    private Asn createAsn(String status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 2, 10, 0);
        return new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                status,
                "메모",
                3,
                now,
                now,
                "SELLER-001",
                "CONK",
                now.minusHours(3),
                null
        );
    }
}


