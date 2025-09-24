package com.bound4.project.config.security;

import com.bound4.project.application.service.PermissionService;
import org.springframework.context.annotation.Configuration;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

@Configuration
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private final PermissionService permissionService;

    public CustomMethodSecurityExpressionHandler(PermissionService permissionService) {
        this.permissionService = permissionService;
        setTrustResolver(new AuthenticationTrustResolverImpl());
    }

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, 
            MethodInvocation methodInvocation) {
        
        CustomSecurityExpressionRoot root = new CustomSecurityExpressionRoot(authentication, permissionService);
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(getRoleHierarchy());
        
        return root;
    }
}