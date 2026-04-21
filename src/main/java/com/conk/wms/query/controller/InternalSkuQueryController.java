package com.conk.wms.query.controller;

import com.conk.wms.command.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 서비스 간 내부 호출 전용 — SKU 등록 여부를 확인하는 경량 엔드포인트다.
@Slf4j
@RestController
@RequestMapping("/wms/internal")
@RequiredArgsConstructor
public class InternalSkuQueryController {

    private final ProductRepository productRepository;

    /**
     * SKU가 WMS에 등록되어 있는지 확인한다.
     * GET /wms/internal/skus/{sku}/exists
     */
    @GetMapping("/skus/{sku}/exists")
    public ResponseEntity<Boolean> skuExists(@PathVariable String sku) {
        boolean exists = productRepository.existsBySkuId(sku);
        log.debug("SKU 존재 여부 조회: sku={}, exists={}", sku, exists);
        return ResponseEntity.ok(exists);
    }
}
