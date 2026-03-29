package com.conk.wms.command.application;

import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class RegisterAsnService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;

    public RegisterAsnService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                              WarehouseRepository warehouseRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public void register(RegisterAsnCommand command) {
        if (!warehouseRepository.existsById(command.getWarehouseId())) {
            throw new IllegalArgumentException("존재하지 않는 창고입니다: " + command.getWarehouseId());
        }
        if (asnRepository.existsByAsnId(command.getAsnId())) {
            throw new IllegalArgumentException("이미 존재하는 ASN 번호입니다: " + command.getAsnId());
        }

        Set<String> seen = new HashSet<>();
        command.getItems().forEach(item -> {
            if (!seen.add(item.getSku())) {
                throw new IllegalArgumentException("중복된 SKU 입니다: " + item.getSku());
            }
        });

        asnRepository.save(new Asn(
                command.getAsnId(),
                command.getWarehouseId(),
                command.getSellerId(),
                command.getExpectedDate(),
                "REGISTERED"
        ));

        command.getItems().forEach(item ->
                asnItemRepository.save(new AsnItem(command.getAsnId(), item.getSku(), item.getQuantity()))
        );
    }
}