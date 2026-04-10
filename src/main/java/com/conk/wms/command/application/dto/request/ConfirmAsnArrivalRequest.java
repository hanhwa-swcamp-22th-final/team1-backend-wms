package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ConfirmAsnArrivalRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// ASN 도착 확인 요청.
// 프론트가 도착 시각을 직접 보내면 그 값을 사용하고, 없으면 서버 현재 시각으로 기록한다.
public class ConfirmAsnArrivalRequest {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivedAt;
}

