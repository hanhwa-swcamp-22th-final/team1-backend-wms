package com.conk.wms.command.controller.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class AssignAsnPutawayRequest {

    private List<ItemRequest> items;

    @Getter
    public static class ItemRequest {
        private String skuId;
        private String locationId;
    }
}
