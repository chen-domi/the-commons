package com.thecommons.backend.config;

import com.thecommons.backend.auth.BcOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BcOidcUserService bcOidcUserService,
            @Value("${app.frontend-url}") String frontendUrl)
            throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/auth/logout"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/inventory", "/api/inventory/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/admin/organizations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/inventory", "/api/inventory/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/inventory/**").authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(bcOidcUserService))
                        .defaultSuccessUrl(frontendUrl, true))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())));

        return http.build();
    }
}
