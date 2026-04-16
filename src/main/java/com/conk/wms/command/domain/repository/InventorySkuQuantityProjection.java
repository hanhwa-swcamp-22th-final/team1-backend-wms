package com.conk.wms.command.domain.repository;

/**
 * SKU별 가용 재고 합계를 조회할 때 사용하는 projection이다.
 */
public interface InventorySkuQuantityProjection {

    String getSku();

    Long getAvailableQuantity();
}
