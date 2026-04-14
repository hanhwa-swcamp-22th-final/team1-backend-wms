package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * integration-service에 개별 송장 발행을 요청할 때 사용하는 DTO다.
 */
@Getter
@Builder
public class IssueLabelRequestDto {

    private String orderId;
    private String carrier;
    private String service;
    private String labelFormat;
    private AddressDto toAddress;
    private AddressDto fromAddress;
    private ParcelDto parcel;

    @Getter
    @Builder
    public static class AddressDto {
        private String name;
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String zip;
        private String country;
        private String phone;
        private String email;
    }

    @Getter
    @Builder
    public static class ParcelDto {
        private Double weight;
        private Double length;
        private Double width;
        private Double height;
    }
}
