package com.conk.wms.common.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AuthContextArgumentResolverTest {

    @Test
    @DisplayName("AuthContext 타입 파라미터를 지원한다")
    void supportsParameter_whenAuthContextType_returnsTrue() throws NoSuchMethodException {
        AuthContextArgumentResolver resolver = new AuthContextArgumentResolver(new AuthContextResolver(true));
        Method method = SampleController.class.getDeclaredMethod("sample", AuthContext.class, String.class);
        MethodParameter authParameter = new MethodParameter(method, 0);
        MethodParameter stringParameter = new MethodParameter(method, 1);

        assertThat(resolver.supportsParameter(authParameter)).isTrue();
        assertThat(resolver.supportsParameter(stringParameter)).isFalse();
    }

    @Test
    @DisplayName("HTTP 헤더로부터 AuthContext를 주입한다")
    void resolveArgument_returnsResolvedAuthContext() throws NoSuchMethodException {
        AuthContextArgumentResolver resolver = new AuthContextArgumentResolver(new AuthContextResolver(true));
        Method method = SampleController.class.getDeclaredMethod("sample", AuthContext.class, String.class);
        MethodParameter authParameter = new MethodParameter(method, 0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, "user-001");
        request.addHeader(AuthHeaders.ROLE, "WM_WORKER");
        request.addHeader(AuthHeaders.TENANT_ID, "tenant-001");

        AuthContext resolved = (AuthContext) resolver.resolveArgument(
                authParameter,
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(resolved.getUserId()).isEqualTo("user-001");
        assertThat(resolved.getRole()).isEqualTo(AuthRole.WM_WORKER);
        assertThat(resolved.getTenantId()).isEqualTo("tenant-001");
    }

    @SuppressWarnings("unused")
    private static final class SampleController {
        private void sample(AuthContext authContext, String plainText) {
        }
    }
}
