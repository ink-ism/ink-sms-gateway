package com.ink.admin.config;

import com.ink.admin.filter.AdminAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Web安全配置
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final AdminAuthFilter adminAuthFilter;

    public WebSecurityConfig(AdminAuthFilter adminAuthFilter) {
        this.adminAuthFilter = adminAuthFilter;
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/login", "/api/admin/reset-password", "/actuator/**", "/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(adminAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
