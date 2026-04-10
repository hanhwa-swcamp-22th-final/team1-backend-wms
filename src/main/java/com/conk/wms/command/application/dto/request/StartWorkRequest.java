package com.conk.wms.command.application.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * StartWorkRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
// 작업 시작 요청 바디.
public class StartWorkRequest {
    private String workerId;
}

