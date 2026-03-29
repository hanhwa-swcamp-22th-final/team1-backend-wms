package com.conk.wms.command.application;

import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterAsnServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private RegisterAsnService registerAsnService;

    @Test
    @DisplayName("ASN 등록 성공: ASN과 품목이 각각 저장된다")
    void register_success() {
        // given
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001", "WH-001", "SELLER-001",
                LocalDate.of(2026, 3, 29),
                List.of(new RegisterAsnItemCommand("SKU-001", 100))
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(asnRepository.existsByAsnId("ASN-001")).thenReturn(false);

        // when
        registerAsnService.register(command);

        // then
        verify(asnRepository, times(1)).save(any(Asn.class));
        verify(asnItemRepository, times(1)).save(any(AsnItem.class));
    }

    @Test
    @DisplayName("ASN 등록 실패: 존재하지 않는 창고면 예외가 발생한다")
    void register_whenWarehouseNotFound_thenThrow() {
        // given
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001", "WH-999", "SELLER-001",
                LocalDate.of(2026, 3, 29),
                List.of(new RegisterAsnItemCommand("SKU-001", 100))
        );
        when(warehouseRepository.existsById("WH-999")).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> registerAsnService.register(command));
    }

    @Test
    @DisplayName("ASN 등록 실패: 이미 존재하는 ASN 번호면 예외가 발생한다")
    void register_whenAsnAlreadyExists_thenThrow() {
        // given
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001", "WH-001", "SELLER-001",
                LocalDate.of(2026, 3, 29),
                List.of(new RegisterAsnItemCommand("SKU-001", 100))
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(asnRepository.existsByAsnId("ASN-001")).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> registerAsnService.register(command));
    }

    @Test
    @DisplayName("ASN 등록 실패: 동일 커맨드 내 중복 SKU가 있으면 예외가 발생한다")
    void register_whenDuplicateSku_thenThrow() {
        // given
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001", "WH-001", "SELLER-001",
                LocalDate.of(2026, 3, 29),
                List.of(
                        new RegisterAsnItemCommand("SKU-001", 100),
                        new RegisterAsnItemCommand("SKU-001", 50)
                )
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(asnRepository.existsByAsnId("ASN-001")).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> registerAsnService.register(command));
    }
}
