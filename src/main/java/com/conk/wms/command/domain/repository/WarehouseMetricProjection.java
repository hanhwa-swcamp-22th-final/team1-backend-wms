package com.conk.wms.command.domain.repository;

/**
 * 창고별 수치 집계를 조회할 때 사용하는 projection이다.
 */
public interface WarehouseMetricProjection {

    String getWarehouseId();

    Long getMetricValue();
}
