package com.example.demo.security;

import com.example.demo.config.JwtAuthenticationFilter;
import com.example.demo.config.SmartschoolAuthenticationFilter;
import com.example.demo.config.CustomAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SmartschoolAuthenticationFilter smartschoolAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            SmartschoolAuthenticationFilter smartschoolAuthenticationFilter,
            CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.smartschoolAuthenticationFilter = smartschoolAuthenticationFilter;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exc -> exc
                        .accessDeniedHandler(customAccessDeniedHandler))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/smartschool-login").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/validate-token").permitAll()
                        .requestMatchers("/api/auth/refresh-token").authenticated()
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/setup").permitAll()
                        .requestMatchers("/api/admin/setup-status").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/admin/genres", "/api/admin/genres/**").authenticated()

                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/uitleningen/all-active").hasAnyRole("BIBBEHEERDER", "SUPER_ADMIN")
                        .requestMatchers("/api/uitleningen/inspectie/conditie").hasAnyRole("BIBBEHEERDER", "SUPER_ADMIN")
                        
                        .requestMatchers("/api/proxy/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(smartschoolAuthenticationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toCollection(java.util.ArrayList::new));

        if (origins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins must not be empty");
        }
        for (String origin : origins) {
            if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                throw new IllegalStateException(
                        "app.cors.allowed-origins contains an invalid origin (must include scheme): " + origin);
            }
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-User-Role", "X-User-Sub", "X-User-Name"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}