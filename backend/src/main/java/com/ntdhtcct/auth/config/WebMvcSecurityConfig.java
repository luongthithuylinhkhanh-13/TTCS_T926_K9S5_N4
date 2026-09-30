package com.ntdhtcct.auth.config;

import com.ntdhtcct.auth.interceptor.ProjectAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Đăng ký Middleware Authorization Interceptor vào chuỗi lọc Spring MVC.
 */
@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final ProjectAuthorizationInterceptor projectAuthorizationInterceptor;

    public WebMvcSecurityConfig(ProjectAuthorizationInterceptor projectAuthorizationInterceptor) {
        this.projectAuthorizationInterceptor = projectAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(projectAuthorizationInterceptor)
                .addPathPatterns("/api/projects/**");
    }
}
