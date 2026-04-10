package com.conk.wms.command.application.dto.request;

/**
 * 창고별 담당 관리자 배정/변경 요청 DTO다.
 */
public class AssignWarehouseManagerRequest {

    private String managerAccountId;
    private String managerName;
    private String managerEmail;
    private String managerPhone;
    private String managerStatus;
    private String lastLoginAt;

    public String getManagerAccountId() {
        return managerAccountId;
    }

    public String getManagerName() {
        return managerName;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public String getManagerPhone() {
        return managerPhone;
    }

    public String getManagerStatus() {
        return managerStatus;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }
}

