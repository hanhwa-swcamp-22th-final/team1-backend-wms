package com.conk.wms.query.service;

import com.conk.wms.query.controller.dto.response.OptionItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductOptionsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 셀러 상품 등록 화면에서 사용하는 옵션을 구성하는 조회 서비스다.
 * 시간 제약으로 고정값을 반환한다.
 */
@Service
public class GetSellerProductOptionsService {

    private static final List<OptionItemResponse> CATEGORIES = List.of(
            new OptionItemResponse("ELECTRONICS", "전자기기"),
            new OptionItemResponse("FOOD", "식품"),
            new OptionItemResponse("FASHION", "패션/의류"),
            new OptionItemResponse("BEAUTY", "뷰티"),
            new OptionItemResponse("HOME", "홈/생활"),
            new OptionItemResponse("SPORTS", "스포츠/레저"),
            new OptionItemResponse("BOOKS", "도서/문구"),
            new OptionItemResponse("TOYS", "장난감/완구")
    );

    private static final List<OptionItemResponse> HS_CODES = List.of(
            new OptionItemResponse("8517", "8517 - 스마트폰/통신기기"),
            new OptionItemResponse("6109", "6109 - 의류(티셔츠)"),
            new OptionItemResponse("3304", "3304 - 화장품"),
            new OptionItemResponse("1905", "1905 - 제과/베이커리"),
            new OptionItemResponse("9503", "9503 - 완구"),
            new OptionItemResponse("6403", "6403 - 신발")
    );

    private static final List<OptionItemResponse> ORIGIN_COUNTRIES = List.of(
            new OptionItemResponse("KR", "대한민국"),
            new OptionItemResponse("US", "미국"),
            new OptionItemResponse("JP", "일본"),
            new OptionItemResponse("CN", "중국"),
            new OptionItemResponse("DE", "독일"),
            new OptionItemResponse("VN", "베트남"),
            new OptionItemResponse("IT", "이탈리아")
    );

    public SellerProductOptionsResponse getOptions(String sellerId) {
        return SellerProductOptionsResponse.builder()
                .categories(CATEGORIES)
                .hsCodes(HS_CODES)
                .originCountries(ORIGIN_COUNTRIES)
                .build();
    }
}
