package com.conk.wms.command.application.dto;

import java.util.List;

/**
 * SaveAsnInspectionCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class SaveAsnInspectionCommand {

    private final String asnId;
    private final String tenantCode;
    private final List<ItemCommand> items;

    public SaveAsnInspectionCommand(String asnId, String tenantCode, List<ItemCommand> items) {
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
        private final int inspectedQuantity;
        private final int defectiveQuantity;
        private final String defectReason;
        private final int putawayQuantity;

        public ItemCommand(String skuId, String locationId, int inspectedQuantity,
                           int defectiveQuantity, String defectReason, int putawayQuantity) {
            this.skuId = skuId;
            this.locationId = locationId;
            this.inspectedQuantity = inspectedQuantity;
            this.defectiveQuantity = defectiveQuantity;
            this.defectReason = defectReason;
            this.putawayQuantity = putawayQuantity;
        }

        public String getSkuId() {
            return skuId;
        }

        public String getLocationId() {
            return locationId;
        }

        public int getInspectedQuantity() {
            return inspectedQuantity;
        }

        public int getDefectiveQuantity() {
            return defectiveQuantity;
        }

        public String getDefectReason() {
            return defectReason;
        }

        public int getPutawayQuantity() {
            return putawayQuantity;
        }
    }
}

