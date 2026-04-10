package com.conk.wms.common.auth;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;

/**
 * Ingress가 주입한 사용자/권한 컨텍스트를 컨트롤러와 서비스에서 공통으로 전달한다.
 */
public final class AuthContext {

    private final String userId;
    private final String userName;
    private final AuthRole role;
    private final String tenantId;
    private final String sellerId;

    public AuthContext(String userId, String userName, AuthRole role, String tenantId, String sellerId) {
        this.userId = normalize(userId);
        this.userName = normalize(userName);
        this.role = role;
        this.tenantId = normalize(tenantId);
        this.sellerId = normalize(sellerId);
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public AuthRole getRole() {
        return role;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String requireUserId() {
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_ID_REQUIRED);
        }
        return userId;
    }

    public AuthRole requireRole() {
        if (role == null) {
            throw new BusinessException(ErrorCode.AUTH_ROLE_REQUIRED);
        }
        return role;
    }

    public String requireTenantId() {
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.AUTH_TENANT_ID_REQUIRED);
        }
        return tenantId;
    }

    public String requireSellerId() {
        if (sellerId == null) {
            throw new BusinessException(ErrorCode.AUTH_SELLER_ID_REQUIRED);
        }
        return sellerId;
    }

    public void requireSellerAccess() {
        AuthRole requiredRole = requireRole();
        if (!requiredRole.isSellerRole()) {
            throw new BusinessException(ErrorCode.AUTH_ROLE_FORBIDDEN);
        }
        requireUserId();
        requireSellerId();
    }

    public void requireWarehouseAccess() {
        AuthRole requiredRole = requireRole();
        if (!requiredRole.isWarehouseRole()) {
            throw new BusinessException(ErrorCode.AUTH_ROLE_FORBIDDEN);
        }
        requireUserId();
        requireTenantId();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
