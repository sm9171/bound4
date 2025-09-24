package com.bound4.project.config.security;

import com.bound4.project.application.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class MethodSecurityConfig {

    private final PermissionService permissionService;

    public MethodSecurityConfig(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new CustomMethodSecurityExpressionHandler(permissionService);
    }
}