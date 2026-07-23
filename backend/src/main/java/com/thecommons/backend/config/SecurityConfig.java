package com.thecommons.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/inventory", "/api/inventory/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/inventory", "/api/inventory/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/inventory", "/api/inventory/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/inventory/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }
}
