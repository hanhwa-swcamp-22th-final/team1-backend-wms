package com.conk.wms.common.auth;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthContextTest {

    @Test
    @DisplayName("seller 컨텍스트는 seller 접근 검증을 통과한다")
    void requireSellerAccess_withSellerContext_succeeds() {
        AuthContext context = new AuthContext("user-001", "Seller User", AuthRole.SELLER, null, "seller-001");

        context.requireSellerAccess();

        assertThat(context.requireSellerId()).isEqualTo("seller-001");
    }

    @Test
    @DisplayName("창고 권한 컨텍스트는 warehouse 접근 검증을 통과한다")
    void requireWarehouseAccess_withWarehouseContext_succeeds() {
        AuthContext context = new AuthContext("user-002", "Manager User", AuthRole.WH_MANAGER, "tenant-001", null);

        context.requireWarehouseAccess();

        assertThat(context.requireTenantId()).isEqualTo("tenant-001");
    }

    @Test
    @DisplayName("seller 컨텍스트에 seller 식별값이 없으면 예외를 던진다")
    void requireSellerAccess_withoutSellerId_throwsBusinessException() {
        AuthContext context = new AuthContext("user-001", "Seller User", AuthRole.SELLER, null, null);

        assertThatThrownBy(context::requireSellerAccess)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_SELLER_ID_REQUIRED));
    }

    @Test
    @DisplayName("seller 역할로 warehouse 접근을 시도하면 예외를 던진다")
    void requireWarehouseAccess_withSellerRole_throwsBusinessException() {
        AuthContext context = new AuthContext("user-001", "Seller User", AuthRole.SELLER, null, "seller-001");

        assertThatThrownBy(context::requireWarehouseAccess)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ROLE_FORBIDDEN));
    }
}
