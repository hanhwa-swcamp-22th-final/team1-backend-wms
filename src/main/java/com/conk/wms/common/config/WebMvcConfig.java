package com.conk.wms.common.config;

import com.conk.wms.common.auth.AuthContextArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 공통 MVC 확장 포인트를 등록한다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectProvider<AuthContextArgumentResolver> authContextArgumentResolverProvider;

    public WebMvcConfig(ObjectProvider<AuthContextArgumentResolver> authContextArgumentResolverProvider) {
        this.authContextArgumentResolverProvider = authContextArgumentResolverProvider;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        AuthContextArgumentResolver authContextArgumentResolver =
                authContextArgumentResolverProvider.getIfAvailable();

        if (authContextArgumentResolver != null) {
            resolvers.add(authContextArgumentResolver);
        }
    }
}
