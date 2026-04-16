package com.conk.wms.command.domain.repository;

/**
 * location별 재고 수량 합계를 조회할 때 사용하는 projection이다.
 */
public interface LocationQuantityProjection {

    String getLocationId();

    Integer getUsedQuantity();
}
