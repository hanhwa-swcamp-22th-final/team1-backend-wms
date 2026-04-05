package com.conk.wms.command.dto;

import java.util.List;

/**
 * AssignAsnPutawayCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class AssignAsnPutawayCommand {

    private final String asnId;
    private final String tenantCode;
    private final List<ItemCommand> items;

    public AssignAsnPutawayCommand(String asnId, String tenantCode, List<ItemCommand> items) {
        this.asnId = asnId;
        this.tenantCode = tenantCode;
        this.items = items;
    }

    public String getAsnId() {
        return asnId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public List<ItemCommand> getItems() {
        return items;
    }

    public static class ItemCommand {
        private final String skuId;
        private final String locationId;

        public ItemCommand(String skuId, String locationId) {
            this.skuId = skuId;
            this.locationId = locationId;
        }

        public String getSkuId() {
            return skuId;
        }

        public String getLocationId() {
            return locationId;
        }
    }
}
