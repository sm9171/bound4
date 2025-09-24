package com.bound4.project.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (JWT 토큰 사용)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 관리: STATELESS (JWT 토큰 기반)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 보안 헤더 설정
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // H2 콘솔용
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(referrer -> 
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            
            // 권한 부여 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // 개발용
                .requestMatchers("/error").permitAll()
                
                // 관리자 전용 엔드포인트
                .requestMatchers("/admin/**").hasRole("A")
                .requestMatchers(HttpMethod.POST, "/api/permissions/**").hasAuthority("MANAGE_PERMISSIONS")
                .requestMatchers(HttpMethod.PUT, "/api/permissions/**").hasAuthority("MANAGE_PERMISSIONS")
                .requestMatchers(HttpMethod.DELETE, "/api/permissions/**").hasAuthority("MANAGE_PERMISSIONS")
                .requestMatchers("/api/policies/**").hasAuthority("MANAGE_POLICIES")
                .requestMatchers("/api/users/*/role").hasAuthority("MANAGE_USERS")
                
                // 프로젝트 관련 엔드포인트
                .requestMatchers(HttpMethod.GET, "/api/projects/**").hasAnyRole("A", "B", "C", "D")
                .requestMatchers(HttpMethod.POST, "/api/projects").hasAnyRole("A", "B", "C", "D")
                .requestMatchers(HttpMethod.PUT, "/api/projects/**").hasAnyRole("A", "B", "C")
                .requestMatchers(HttpMethod.DELETE, "/api/projects/**").hasAnyRole("A", "B")
                
                // 사용자 관련 엔드포인트
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("A", "B", "C", "D")
                .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("A", "B", "C", "D")
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("A", "B")
                
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 오리진 (개발 환경에서는 모든 오리진 허용, 프로덕션에서는 특정 도메인만)
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept",
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));
        
        // 자격 증명 허용
        configuration.setAllowCredentials(true);
        
        // 프리플라이트 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}