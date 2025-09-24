package com.bound4.project.config.security;

import com.bound4.project.domain.Role;
import com.bound4.project.domain.SubscriptionPlan;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret:bound4-default-secret-key-for-jwt-token-generation-and-validation}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 24시간 (ms)
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration:604800000}") // 7일 (ms)
    private long jwtRefreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(String email, Long userId, Role role, SubscriptionPlan subscriptionPlan) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .claim("subscriptionPlan", subscriptionPlan.name())
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(String email, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        
        String email = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String roleStr = claims.get("role", String.class);
        String subscriptionPlanStr = claims.get("subscriptionPlan", String.class);

        Collection<? extends GrantedAuthority> authorities = getAuthorities(roleStr);

        UserDetails userDetails = User.builder()
                .username(email)
                .password("") // 토큰 기반이므로 비밀번호 불필요
                .authorities(authorities)
                .build();

        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
        
        // 추가 정보를 details에 저장
        JwtAuthenticationDetails details = new JwtAuthenticationDetails(userId, 
                Role.valueOf(roleStr), SubscriptionPlan.valueOf(subscriptionPlanStr));
        authentication.setDetails(details);

        return authentication;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(String role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public Role getRoleFromToken(String token) {
        String roleStr = getClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public SubscriptionPlan getSubscriptionPlanFromToken(String token) {
        String planStr = getClaims(token).get("subscriptionPlan", String.class);
        return SubscriptionPlan.valueOf(planStr);
    }

    public boolean isAccessToken(String token) {
        String type = getClaims(token).get("type", String.class);
        return "ACCESS".equals(type);
    }

    public boolean isRefreshToken(String token) {
        String type = getClaims(token).get("type", String.class);
        return "REFRESH".equals(type);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("JWT 토큰이 유효하지 않습니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims 문자열이 비어있습니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 오류가 발생했습니다: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Duration getTokenRemainingTime(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Duration.ofMillis(Math.max(0, remaining));
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static class JwtAuthenticationDetails {
        private final Long userId;
        private final Role role;
        private final SubscriptionPlan subscriptionPlan;

        public JwtAuthenticationDetails(Long userId, Role role, SubscriptionPlan subscriptionPlan) {
            this.userId = userId;
            this.role = role;
            this.subscriptionPlan = subscriptionPlan;
        }

        public Long getUserId() {
            return userId;
        }

        public Role getRole() {
            return role;
        }

        public SubscriptionPlan getSubscriptionPlan() {
            return subscriptionPlan;
        }
    }
}