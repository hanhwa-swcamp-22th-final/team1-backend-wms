package com.conk.wms.common.auth;

/**
 * Ingress가 WMS로 전달하는 인증/권한 헤더 이름 모음이다.
 */
public final class AuthHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_NAME = "X-User-Name";
    public static final String ROLE = "X-Role";
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String SELLER_ID = "X-Seller-Id";
    public static final String LEGACY_TENANT_CODE = "X-Tenant-Code";

    private AuthHeaders() {
    }
}
