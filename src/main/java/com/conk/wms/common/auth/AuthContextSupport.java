package com.conk.wms.common.auth;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;

/**
 * 컨트롤러가 AuthContext를 기존 tenant header 의미와 호환되게 읽도록 돕는 전환용 유틸이다.
 */
public final class AuthContextSupport {

    private AuthContextSupport() {
    }

    public static String resolveSellerId(AuthContext authContext) {
        if (authContext == null || isBlank(authContext.getSellerId())) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return authContext.getSellerId();
    }

    public static String resolveTenantId(AuthContext authContext) {
        if (authContext == null || isBlank(authContext.getTenantId())) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return authContext.getTenantId();
    }

    public static String resolveActorId(AuthContext authContext) {
        if (authContext == null) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }

        if (!isBlank(authContext.getUserId())) {
            return authContext.getUserId();
        }

        if (!isBlank(authContext.getTenantId())) {
            return authContext.getTenantId();
        }

        if (!isBlank(authContext.getSellerId())) {
            return authContext.getSellerId();
        }

        throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
