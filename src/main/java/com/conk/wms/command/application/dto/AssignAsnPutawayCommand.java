package com.conk.wms.command.application.dto;

import java.util.List;

/**
 * AssignAsnPutawayCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class AssignAsnPutawayCommand {

    private final String asnId;
    private final String tenantId;
    private final String actorId;
    private final List<ItemCommand> items;

    public AssignAsnPutawayCommand(String asnId, String tenantId, String actorId, List<ItemCommand> items) {
        this.asnId = asnId;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.items = items;
    }

    public String getAsnId() {
        return asnId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActorId() {
        return actorId;
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

