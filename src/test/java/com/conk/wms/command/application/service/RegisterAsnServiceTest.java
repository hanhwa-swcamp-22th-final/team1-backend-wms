package com.conk.wms.command.application.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 서비스 테스트는 DB 없이 "검증 -> 도메인 저장 호출" 흐름이 맞는지만 본다.
class RegisterAsnServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private SellerWarehouseRepository sellerWarehouseRepository;

    @InjectMocks
    private RegisterAsnService registerAsnService;

    @Test
    @DisplayName("ASN 등록 성공: ASN과 품목 상세가 저장된다")
    void register_success() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "온도 민감 상품 포함",
                List.of(new RegisterAsnItemCommand("SKU-001", "루미에르 앰플 30ml", 100, 5))
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(sellerWarehouseRepository.existsByIdSellerIdAndIdWarehouseId("SELLER-001", "WH-001")).thenReturn(true);
        when(asnRepository.existsByAsnId("ASN-001")).thenReturn(false);

        // 실제 저장소 구현 대신 mock에 저장되는 값을 캡처해, 서비스가 어떤 상태를 만들었는지 검증한다.
        registerAsnService.register(command);

        ArgumentCaptor<Asn> asnCaptor = ArgumentCaptor.forClass(Asn.class);
        ArgumentCaptor<AsnItem> itemCaptor = ArgumentCaptor.forClass(AsnItem.class);

        verify(asnRepository, times(1)).save(asnCaptor.capture());
        verify(asnItemRepository, times(1)).save(itemCaptor.capture());

        Asn savedAsn = asnCaptor.getValue();
        AsnItem savedItem = itemCaptor.getValue();

        assertEquals("ASN-001", savedAsn.getAsnId());
        assertEquals("WH-001", savedAsn.getWarehouseId());
        assertEquals("SELLER-001", savedAsn.getSellerId());
        assertEquals(LocalDate.of(2026, 3, 29), savedAsn.getExpectedArrivalDate());
        assertEquals("REGISTERED", savedAsn.getStatus());
        assertEquals("온도 민감 상품 포함", savedAsn.getSellerMemo());
        assertEquals(5, savedAsn.getBoxQuantity());

        assertEquals("ASN-001", savedItem.getAsnId());
        assertEquals("SKU-001", savedItem.getSkuId());
        assertEquals("루미에르 앰플 30ml", savedItem.getProductNameSnapshot());
        assertEquals(100, savedItem.getQuantity());
        assertEquals(5, savedItem.getBoxQuantity());
    }

    @Test
    @DisplayName("ASN 등록 실패: 존재하지 않는 창고면 예외가 발생한다")
    void register_whenWarehouseNotFound_thenThrow() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-999",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "메모",
                List.of(new RegisterAsnItemCommand("SKU-001", "상품", 100, 5))
        );
        when(warehouseRepository.existsById("WH-999")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> registerAsnService.register(command));
        assertEquals(ErrorCode.ASN_WAREHOUSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("ASN 등록 실패: 셀러와 연결되지 않은 창고면 예외가 발생한다")
    void register_whenSellerWarehouseMismatch_thenThrow() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "메모",
                List.of(new RegisterAsnItemCommand("SKU-001", "상품", 100, 5))
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(sellerWarehouseRepository.existsByIdSellerIdAndIdWarehouseId("SELLER-001", "WH-001")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> registerAsnService.register(command));
        assertEquals(ErrorCode.ASN_SELLER_WAREHOUSE_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("ASN 등록 실패: 이미 존재하는 ASN 번호면 예외가 발생한다")
    void register_whenAsnAlreadyExists_thenThrow() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "메모",
                List.of(new RegisterAsnItemCommand("SKU-001", "상품", 100, 5))
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(sellerWarehouseRepository.existsByIdSellerIdAndIdWarehouseId("SELLER-001", "WH-001")).thenReturn(true);
        when(asnRepository.existsByAsnId("ASN-001")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> registerAsnService.register(command));
        assertEquals(ErrorCode.ASN_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("ASN 등록 실패: 동일 커맨드 내 중복 SKU가 있으면 예외가 발생한다")
    void register_whenDuplicateSku_thenThrow() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "메모",
                List.of(
                        new RegisterAsnItemCommand("SKU-001", "상품A", 100, 5),
                        new RegisterAsnItemCommand("SKU-001", "상품B", 50, 2)
                )
        );
        when(warehouseRepository.existsById("WH-001")).thenReturn(true);
        when(sellerWarehouseRepository.existsByIdSellerIdAndIdWarehouseId("SELLER-001", "WH-001")).thenReturn(true);
        when(asnRepository.existsByAsnId("ASN-001")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> registerAsnService.register(command));
        assertEquals(ErrorCode.ASN_DUPLICATE_SKU, exception.getErrorCode());
    }

    @Test
    @DisplayName("ASN 등록 실패: 품목이 비어 있으면 예외가 발생한다")
    void register_whenItemsEmpty_thenThrow() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "메모",
                List.of()
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> registerAsnService.register(command));
        assertEquals(ErrorCode.ASN_ITEMS_REQUIRED, exception.getErrorCode());
        verify(asnRepository, never()).save(any(Asn.class));
        verify(asnItemRepository, never()).save(any(AsnItem.class));
    }

    @Test
    @DisplayName("ASN 등록 실패: 박스 수가 0 이하면 예외가 발생한다")
    void register_whenBoxQuantityInvalid_thenThrow() {
        RegisterAsnCommand command = new RegisterAsnCommand(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "메모",
                List.of(new RegisterAsnItemCommand("SKU-001", "상품", 100, 0))
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> registerAsnService.register(command));
        assertEquals(ErrorCode.ASN_INVALID_BOX_QUANTITY, exception.getErrorCode());
        verify(asnRepository, never()).save(any(Asn.class));
        verify(asnItemRepository, never()).save(any(AsnItem.class));
    }
}


