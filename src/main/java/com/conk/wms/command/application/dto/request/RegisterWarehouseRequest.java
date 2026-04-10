package com.conk.wms.command.application.dto.request;

/**
 * 창고 등록 화면에서 넘어오는 신규 창고 등록 요청 DTO다.
 */
public class RegisterWarehouseRequest {

    private String name;
    private Integer sqft;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNo;
    private String openTime;
    private String closeTime;
    private String timezone;
    private String managerName;
    private String managerEmail;
    private String managerPhone;

    public String getName() {
        return name;
    }

    public Integer getSqft() {
        return sqft;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public String getOpenTime() {
        return openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public String getTimezone() {
        return timezone;
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
}

