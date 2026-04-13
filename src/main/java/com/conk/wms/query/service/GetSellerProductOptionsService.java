package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.query.controller.dto.response.OptionItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductOptionsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 셀러 상품 등록 화면에서 사용하는 옵션을 구성하는 조회 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerProductOptionsService {

    private static final OptionItemResponse DEFAULT_CATEGORY = new OptionItemResponse("BEAUTY", "뷰티");
    private static final OptionItemResponse DEFAULT_HS_CODE = new OptionItemResponse("330499", "330499");
    private static final OptionItemResponse DEFAULT_ORIGIN = new OptionItemResponse("KR", "대한민국");

    private final ProductRepository productRepository;

    public GetSellerProductOptionsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public SellerProductOptionsResponse getOptions(String sellerId) {
        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);

        Set<OptionItemResponse> categories = new LinkedHashSet<>();
        for (Product product : products) {
            String category = product.getCategoryName();
            if (category == null || category.isBlank()) {
                continue;
            }
            categories.add(new OptionItemResponse(category, category));
        }

        if (categories.isEmpty()) {
            categories.add(DEFAULT_CATEGORY);
        }

        return SellerProductOptionsResponse.builder()
                .categories(List.copyOf(categories))
                .hsCodes(List.of(DEFAULT_HS_CODE))
                .originCountries(List.of(DEFAULT_ORIGIN))
                .build();
    }
}
