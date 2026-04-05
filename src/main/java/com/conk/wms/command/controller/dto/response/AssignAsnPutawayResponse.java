package com.conk.wms.command.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AssignAsnPutawayResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
@AllArgsConstructor
public class AssignAsnPutawayResponse {

    private String asnId;
    private int assignedItemCount;
}
