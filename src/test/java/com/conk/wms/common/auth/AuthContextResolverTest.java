package com.conk.wms.common.auth;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthContextResolverTest {

    @Test
    @DisplayName("Ingress가 주입한 seller 헤더를 AuthContext로 변환한다")
    void resolve_withSellerHeaders_returnsSellerContext() {
        AuthContextResolver resolver = new AuthContextResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, "user-001");
        request.addHeader(AuthHeaders.USER_NAME, "Seller User");
        request.addHeader(AuthHeaders.ROLE, "SELLER");
        request.addHeader(AuthHeaders.SELLER_ID, "seller-001");

        AuthContext context = resolver.resolve(request);

        assertThat(context.getUserId()).isEqualTo("user-001");
        assertThat(context.getUserName()).isEqualTo("Seller User");
        assertThat(context.getRole()).isEqualTo(AuthRole.SELLER);
        assertThat(context.getSellerId()).isEqualTo("seller-001");
        assertThat(context.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Ingress가 주입한 창고 권한 헤더를 AuthContext로 변환한다")
    void resolve_withWarehouseHeaders_returnsWarehouseContext() {
        AuthContextResolver resolver = new AuthContextResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, "user-002");
        request.addHeader(AuthHeaders.USER_NAME, "Manager User");
        request.addHeader(AuthHeaders.ROLE, "WH_MANAGER");
        request.addHeader(AuthHeaders.TENANT_ID, "tenant-001");

        AuthContext context = resolver.resolve(request);

        assertThat(context.getUserId()).isEqualTo("user-002");
        assertThat(context.getUserName()).isEqualTo("Manager User");
        assertThat(context.getRole()).isEqualTo(AuthRole.WH_MANAGER);
        assertThat(context.getTenantId()).isEqualTo("tenant-001");
        assertThat(context.getSellerId()).isNull();
    }

    @Test
    @DisplayName("레거시 tenant 헤더 허용 시 tenant와 seller fallback 값을 채운다")
    void resolve_withLegacyHeader_whenEnabled_populatesFallbackIdentifiers() {
        AuthContextResolver resolver = new AuthContextResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.LEGACY_TENANT_CODE, "legacy-001");

        AuthContext context = resolver.resolve(request);

        assertThat(context.getTenantId()).isEqualTo("legacy-001");
        assertThat(context.getSellerId()).isEqualTo("legacy-001");
    }

    @Test
    @DisplayName("레거시 tenant 헤더 비활성 시 fallback 값을 사용하지 않는다")
    void resolve_withLegacyHeader_whenDisabled_ignoresFallbackIdentifiers() {
        AuthContextResolver resolver = new AuthContextResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.LEGACY_TENANT_CODE, "legacy-001");

        AuthContext context = resolver.resolve(request);

        assertThat(context.getTenantId()).isNull();
        assertThat(context.getSellerId()).isNull();
    }

    @Test
    @DisplayName("지원하지 않는 역할 헤더가 오면 예외를 던진다")
    void resolve_withInvalidRole_throwsBusinessException() {
        AuthContextResolver resolver = new AuthContextResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.ROLE, "UNKNOWN_ROLE");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ROLE_INVALID));
    }
}
