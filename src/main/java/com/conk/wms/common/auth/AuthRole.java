package com.conk.wms.common.auth;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;

import java.util.Locale;

/**
 * Ingress가 주입한 역할 값을 애플리케이션 내부 enum으로 정규화한다.
 */
public enum AuthRole {
    SELLER,
    MASTER_ADMIN,
    WH_MANAGER,
    WH_WORKER;

    public static AuthRole fromHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AuthRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCode.AUTH_ROLE_INVALID,
                    ErrorCode.AUTH_ROLE_INVALID.getMessage() + ": " + value
            );
        }
    }

    public boolean isSellerRole() {
        return this == SELLER;
    }

    public boolean isWarehouseRole() {
        return this == MASTER_ADMIN || this == WH_MANAGER || this == WH_WORKER;
    }
}
