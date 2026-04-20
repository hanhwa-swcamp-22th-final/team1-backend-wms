package com.conk.wms.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HttpServletRequest에서 Ingress 주입 헤더를 읽어 AuthContext로 정규화한다.
 */
@Component
public class AuthContextResolver {

    private final boolean acceptLegacyTenantCode;

    public AuthContextResolver(
            @Value("${app.auth.accept-legacy-tenant-code:true}") boolean acceptLegacyTenantCode
    ) {
        this.acceptLegacyTenantCode = acceptLegacyTenantCode;
    }

    public AuthContext resolve(HttpServletRequest request) {
        String userId = readHeader(request, AuthHeaders.USER_ID);
        String workerCode = readHeader(request, AuthHeaders.WORKER_CODE);
        String userName = readHeader(request, AuthHeaders.USER_NAME);
        AuthRole role = AuthRole.fromHeaderValue(readHeader(request, AuthHeaders.ROLE));
        String tenantId = readHeader(request, AuthHeaders.TENANT_ID);
        String sellerId = readHeader(request, AuthHeaders.SELLER_ID);

        if (acceptLegacyTenantCode) {
            String legacyTenantCode = readHeader(request, AuthHeaders.LEGACY_TENANT_CODE);
            if (userId == null) {
                userId = legacyTenantCode;
            }
            if (tenantId == null) {
                tenantId = legacyTenantCode;
            }
            if (sellerId == null) {
                sellerId = legacyTenantCode;
            }
        }

        if (workerCode == null) {
            workerCode = userId;
        }

        return new AuthContext(userId, workerCode, userName, role, tenantId, sellerId);
    }

    private String readHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
