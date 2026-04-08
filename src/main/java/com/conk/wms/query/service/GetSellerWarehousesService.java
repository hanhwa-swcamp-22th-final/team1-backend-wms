package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 셀러-창고 매핑을 조회하는 내부 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerWarehousesService {

    private final SellerWarehouseRepository sellerWarehouseRepository;

    public GetSellerWarehousesService(SellerWarehouseRepository sellerWarehouseRepository) {
        this.sellerWarehouseRepository = sellerWarehouseRepository;
    }

    public List<SellerWarehouse> getSellerWarehouses(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            return List.of();
        }
        return sellerWarehouseRepository.findAllByIdSellerIdOrderByIsDefaultDescIdWarehouseIdAsc(sellerId);
    }
}
