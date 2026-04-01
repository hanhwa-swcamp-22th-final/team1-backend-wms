package com.conk.wms.query.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.mapper.AsnQueryMapper;
import com.conk.wms.query.controller.dto.response.AsnDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// query 서비스 테스트는 ASN 상세 화면에 필요한 조합/가공 결과가 맞는지 검증한다.
class GetAsnDetailServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Spy
    private AsnQueryMapper asnQueryMapper;

    @InjectMocks
    private GetAsnDetailService getAsnDetailService;

    @Test
    @DisplayName("ASN 상세 조회 시 기본 정보와 품목 상세가 함께 반환된다")
    void getAsnDetail_success() {
        // 상세는 asn 헤더 + asn_item + warehouse를 함께 조합해야 하므로 세 저장소 mock을 모두 준비한다.
        Asn asn = new Asn(
                "ASN-20260329-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                "REGISTERED",
                "온도 민감 상품 포함",
                5,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "SELLER-001",
                "SELLER-001"
        );

        when(asnRepository.findByAsnIdAndSellerId("ASN-20260329-001", "SELLER-001"))
                .thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-20260329-001"))
                .thenReturn(List.of(
                        new AsnItem("ASN-20260329-001", "SKU-001", 100, "루미에르 앰플 30ml", 3),
                        new AsnItem("ASN-20260329-001", "SKU-002", 50, "리페어 마스크팩 10입", 2)
                ));
        when(warehouseRepository.findById("WH-001"))
                .thenReturn(Optional.of(new Warehouse("WH-001", "서울 창고", "SELLER-001")));

        AsnDetailResponse result = getAsnDetailService.getAsnDetail("SELLER-001", "ASN-20260329-001");

        assertEquals("ASN-20260329-001", result.getId());
        assertEquals("ASN-20260329-001", result.getAsnNo());
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals("서울 창고", result.getWarehouse());
        assertEquals(2, result.getSkuCount());
        assertEquals(150, result.getTotalQuantity());
        assertEquals(150, result.getPlannedQty());
        assertEquals("2026-03-30", result.getExpectedDate());
        assertEquals("REF-29-001", result.getReferenceNo());
        assertEquals("온도 민감 상품 포함", result.getNote());

        assertEquals("SELLER-001", result.getDetail().getSupplierName());
        assertEquals(5, result.getDetail().getTotalCartons());
        assertEquals(2, result.getDetail().getItems().size());
        assertEquals("SKU-001", result.getDetail().getItems().get(0).getSku());
        assertEquals("루미에르 앰플 30ml", result.getDetail().getItems().get(0).getProductName());
        assertEquals(100, result.getDetail().getItems().get(0).getQuantity());
        assertEquals(3, result.getDetail().getItems().get(0).getCartons());
    }

    @Test
    @DisplayName("다른 셀러의 ASN 이거나 존재하지 않으면 예외가 발생한다")
    void getAsnDetail_whenAsnNotFound_thenThrow() {
        // seller 범위 밖 데이터 접근을 막는 것도 상세 조회의 핵심 규칙이라 Optional.empty() 케이스를 둔다.
        when(asnRepository.findByAsnIdAndSellerId("ASN-20260329-001", "SELLER-001"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> getAsnDetailService.getAsnDetail("SELLER-001", "ASN-20260329-001")
        );

        assertEquals(ErrorCode.ASN_NOT_FOUND, exception.getErrorCode());
    }
}
