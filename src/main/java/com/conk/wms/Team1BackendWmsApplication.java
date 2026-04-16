package com.conk.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스프링 부트 WMS 애플리케이션의 진입점이다.
 * 프로파일과 설정을 읽어 전체 백엔드 구동을 시작한다.
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.conk.wms.query.client.feign")
@EnableScheduling
public class Team1BackendWmsApplication {

  public static void main(String[] args) {
    SpringApplication.run(Team1BackendWmsApplication.class, args);
  }

}
