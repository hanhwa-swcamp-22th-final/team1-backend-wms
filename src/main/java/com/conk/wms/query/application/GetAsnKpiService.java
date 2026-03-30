package com.conk.wms.query.application;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.query.application.dto.AsnKpiResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
// ASN KPI 조회 전용 query service.
// 현재는 로그인 seller 범위만 집계하며, 상태 체계는 seller 목록 화면에서 쓰는 값으로 맞춘다.
public class GetAsnKpiService {

    private final AsnRepository asnRepository;

    public GetAsnKpiService(AsnRepository asnRepository) {
        this.asnRepository = asnRepository;
    }

    // seller ASN 목록 화면 상단 카드 4개 수치를 계산한다.
    // 현재는 목록 조회와 동일한 seller 범위를 기준으로 간단 집계하는 방식으로 시작한다.
    public AsnKpiResponse getAsnKpi(String sellerId) {
        List<Asn> asns = asnRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);

        int submitted = 0;
        int received = 0;
        int cancelled = 0;

        for (Asn asn : asns) {
            switch (toSellerStatus(asn.getStatus())) {
                case "SUBMITTED" -> submitted++;
                case "RECEIVED" -> received++;
                case "CANCELLED" -> cancelled++;
                default -> {
                    // seller 화면에서 정의하지 않은 상태는 total에만 포함하고 별도 카운트는 하지 않는다.
                }
            }
        }

        return AsnKpiResponse.builder()
                .total(asns.size())
                .submitted(submitted)
                .received(received)
                .cancelled(cancelled)
                .build();
    }

    // KPI 숫자와 목록 화면 상태 탭이 같은 규칙을 쓰도록 상태 매핑 로직을 맞춘다.
    private String toSellerStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SUBMITTED";
        }
        return switch (status) {
            case "REGISTERED" -> "SUBMITTED";
            case "ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED" -> "RECEIVED";
            case "CANCELED", "CANCELLED" -> "CANCELLED";
            default -> status;
        };
    }
}
