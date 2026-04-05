package com.conk.wms.command.service;

import com.conk.wms.command.dto.ChangeProductStatusCommand;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 상태 변경 로직을 처리하는 command 서비스다.
 */
@Service
public class ChangeProductStatusService {

    private final ProductRepository productRepository;

    public ChangeProductStatusService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void changeStatus(ChangeProductStatusCommand command) {
        Product product = productRepository.findBySku(command.getSku())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + command.getSku()));

        product.changeStatus(command.getStatus());
        productRepository.save(product);
    }
}
