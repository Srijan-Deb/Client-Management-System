package com.cms.account.config;

import com.cms.account.filter.UserSyncFilter;
import com.cms.common.security.CmsJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserSyncFilter userSyncFilter;

    public SecurityConfig(UserSyncFilter userSyncFilter) {
        this.userSyncFilter = userSyncFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                            // Internal service-to-service endpoint â€” no JWT required.
                            // Called by Client Service during client onboarding.
                            // Protected by Docker network isolation in Phase 2.
                            // TODO Phase 8: replace with mTLS or API-key header auth.
                            .requestMatchers("/api/v1/accounts/link/**").permitAll()
                            .anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> {
                        oauth2.jwt(jwt -> {
                                jwt.jwtAuthenticationConverter(new CmsJwtAuthenticationConverter());
                        });
                })
                // UserSyncFilter only applies to authenticated (JWT-bearing) requests
                .addFilterAfter(userSyncFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
