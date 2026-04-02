package com.conk.wms.command.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssignAsnPutawayResponse {

    private String asnId;
    private int assignedItemCount;
}
