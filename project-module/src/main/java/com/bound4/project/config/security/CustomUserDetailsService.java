package com.bound4.project.config.security;

import com.bound4.project.adapter.out.persistence.UserRepository;
import com.bound4.project.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 정보 로딩: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return createUserDetails(user);
    }

    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("사용자 ID로 정보 로딩: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 ID를 찾을 수 없습니다: " + userId));

        return createUserDetails(user);
    }

    private UserDetails createUserDetails(User user) {
        Collection<GrantedAuthority> authorities = getAuthorities(user);

        return CustomUserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .subscriptionPlan(user.getSubscriptionPlan())
                .authorities(authorities)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    private Collection<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 기본 역할 권한
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        
        // 구독 플랜 기반 권한
        authorities.add(new SimpleGrantedAuthority("PLAN_" + user.getSubscriptionPlan().name()));
        
        // 역할별 세부 권한 추가
        switch (user.getRole()) {
            case A -> {
                authorities.add(new SimpleGrantedAuthority("ADMIN"));
                authorities.add(new SimpleGrantedAuthority("MANAGE_USERS"));
                authorities.add(new SimpleGrantedAuthority("MANAGE_PERMISSIONS"));
                authorities.add(new SimpleGrantedAuthority("MANAGE_POLICIES"));
                authorities.add(new SimpleGrantedAuthority("SYSTEM_ACCESS"));
            }
            case B -> {
                authorities.add(new SimpleGrantedAuthority("PREMIUM_USER"));
                authorities.add(new SimpleGrantedAuthority("ADVANCED_FEATURES"));
                if (user.getSubscriptionPlan().name().equals("PRO")) {
                    authorities.add(new SimpleGrantedAuthority("PRO_FEATURES"));
                }
            }
            case C -> {
                authorities.add(new SimpleGrantedAuthority("STANDARD_USER"));
                if (user.getSubscriptionPlan().name().equals("PRO")) {
                    authorities.add(new SimpleGrantedAuthority("ENHANCED_FEATURES"));
                }
            }
            case D -> authorities.add(new SimpleGrantedAuthority("BASIC_USER"));
        }

        log.debug("사용자 {} 권한: {}", user.getEmail(), authorities);
        return authorities;
    }

    public static class CustomUserPrincipal implements UserDetails {
        private final Long id;
        private final String email;
        private final String password;
        private final com.bound4.project.domain.Role role;
        private final com.bound4.project.domain.SubscriptionPlan subscriptionPlan;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean enabled;
        private final boolean accountNonExpired;
        private final boolean accountNonLocked;
        private final boolean credentialsNonExpired;

        private CustomUserPrincipal(Builder builder) {
            this.id = builder.id;
            this.email = builder.email;
            this.password = builder.password;
            this.role = builder.role;
            this.subscriptionPlan = builder.subscriptionPlan;
            this.authorities = builder.authorities;
            this.enabled = builder.enabled;
            this.accountNonExpired = builder.accountNonExpired;
            this.accountNonLocked = builder.accountNonLocked;
            this.credentialsNonExpired = builder.credentialsNonExpired;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public boolean isAccountNonExpired() {
            return accountNonExpired;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return credentialsNonExpired;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        public Long getId() {
            return id;
        }

        public com.bound4.project.domain.Role getRole() {
            return role;
        }

        public com.bound4.project.domain.SubscriptionPlan getSubscriptionPlan() {
            return subscriptionPlan;
        }

        public static class Builder {
            private Long id;
            private String email;
            private String password;
            private com.bound4.project.domain.Role role;
            private com.bound4.project.domain.SubscriptionPlan subscriptionPlan;
            private Collection<? extends GrantedAuthority> authorities;
            private boolean enabled = true;
            private boolean accountNonExpired = true;
            private boolean accountNonLocked = true;
            private boolean credentialsNonExpired = true;

            public Builder id(Long id) {
                this.id = id;
                return this;
            }

            public Builder email(String email) {
                this.email = email;
                return this;
            }

            public Builder password(String password) {
                this.password = password;
                return this;
            }

            public Builder role(com.bound4.project.domain.Role role) {
                this.role = role;
                return this;
            }

            public Builder subscriptionPlan(com.bound4.project.domain.SubscriptionPlan subscriptionPlan) {
                this.subscriptionPlan = subscriptionPlan;
                return this;
            }

            public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
                this.authorities = authorities;
                return this;
            }

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder accountNonExpired(boolean accountNonExpired) {
                this.accountNonExpired = accountNonExpired;
                return this;
            }

            public Builder accountNonLocked(boolean accountNonLocked) {
                this.accountNonLocked = accountNonLocked;
                return this;
            }

            public Builder credentialsNonExpired(boolean credentialsNonExpired) {
                this.credentialsNonExpired = credentialsNonExpired;
                return this;
            }

            public CustomUserPrincipal build() {
                return new CustomUserPrincipal(this);
            }
        }
    }
}