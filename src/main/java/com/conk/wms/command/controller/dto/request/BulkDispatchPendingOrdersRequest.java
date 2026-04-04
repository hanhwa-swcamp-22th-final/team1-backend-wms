package com.conk.wms.command.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkDispatchPendingOrdersRequest {

    private List<String> orderIds;
    private String pickingGroupBy;
    private String targetTime;
    private boolean sendNotif;
}
