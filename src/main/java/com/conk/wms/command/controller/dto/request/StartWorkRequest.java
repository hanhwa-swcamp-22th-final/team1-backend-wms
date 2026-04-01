package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 작업 시작 요청 바디.
public class StartWorkRequest {
    private String workerId;
}
