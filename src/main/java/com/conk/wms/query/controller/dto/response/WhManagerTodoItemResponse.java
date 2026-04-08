package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 관리자 대시보드의 오늘의 작업 현황 한 줄 응답 DTO다.
 */
@Getter
@Builder
public class WhManagerTodoItemResponse {

    private String text;
    private String time;
    private String color;
    private String badge;
}
