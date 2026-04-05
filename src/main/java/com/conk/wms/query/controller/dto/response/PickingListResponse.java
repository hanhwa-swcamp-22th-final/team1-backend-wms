package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PickingListResponse {

    private String id;
    private String assignedWorker;
    private int orderCount;
    private int itemCount;
    private int completedBins;
    private int totalBins;
    private String issuedAt;
    private String completedAt;
    private String status;
}
