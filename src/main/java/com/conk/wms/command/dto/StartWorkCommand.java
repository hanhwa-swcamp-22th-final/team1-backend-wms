package com.conk.wms.command.dto;

/**
 * StartWorkCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class StartWorkCommand {

    private final String workId;
    private final String workerId;

    public StartWorkCommand(String workId, String workerId) {
        this.workId = workId;
        this.workerId = workerId;
    }

    public String getWorkId() { return workId; }
    public String getWorkerId() { return workerId; }
}
