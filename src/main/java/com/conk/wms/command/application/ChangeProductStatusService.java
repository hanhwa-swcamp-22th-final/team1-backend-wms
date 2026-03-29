package com.conk.wms.command.application;

import com.conk.wms.command.application.dto.ChangeProductStatusCommand;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
