package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.WarehouseRegionSequence;
import com.conk.wms.command.domain.repository.WarehouseRegionSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 창고 번호 생성기다.
 */
@Service
public class WarehouseIdGenerator {

    private final WarehouseRegionSequenceRepository warehouseRegionSequenceRepository;

    public WarehouseIdGenerator(WarehouseRegionSequenceRepository warehouseRegionSequenceRepository) {
        this.warehouseRegionSequenceRepository = warehouseRegionSequenceRepository;
    }

    @Transactional
    public String generate(String state, String city) {
        String regionCode = resolveRegionCode(state, city);
        WarehouseRegionSequence sequence = warehouseRegionSequenceRepository.findByRegionCodeForUpdate(regionCode)
                .orElseGet(() -> warehouseRegionSequenceRepository.save(WarehouseRegionSequence.of(regionCode)));
        int next = sequence.increment();
        return "WH-" + regionCode + "-" + String.format("%03d", next);
    }

    String resolveRegionCode(String state, String city) {
        String normalizedState = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        if ("CA".equals(normalizedState)) {
            return "LAX";
        }
        if ("TX".equals(normalizedState)) {
            return "DFW";
        }
        if ("NY".equals(normalizedState)) {
            return "NYC";
        }
        if ("GA".equals(normalizedState)) {
            return "ATL";
        }

        String normalizedCity = city == null ? "" : city.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (normalizedCity.length() >= 3) {
            return normalizedCity.substring(0, 3);
        }
        if (normalizedState.length() >= 3) {
            return normalizedState.substring(0, 3);
        }
        if (normalizedState.length() == 2) {
            return normalizedState + "X";
        }
        return "GEN";
    }
}
