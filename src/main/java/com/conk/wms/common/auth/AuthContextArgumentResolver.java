package com.conk.wms.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 메서드에서 AuthContext를 직접 주입받을 수 있게 하는 MVC resolver다.
 */
@Component
@ConditionalOnBean(AuthContextResolver.class)
public class AuthContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthContextResolver authContextResolver;

    public AuthContextArgumentResolver(AuthContextResolver authContextResolver) {
        this.authContextResolver = authContextResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        return authContextResolver.resolve(request);
    }
}
