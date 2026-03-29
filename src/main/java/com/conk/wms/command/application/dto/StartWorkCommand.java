package com.conk.wms.command.application.dto;

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
