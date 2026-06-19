package com.project.klare_server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.klare_server.common.config.properties.IdempotencyProperties;
import com.project.klare_server.common.config.properties.RateLimitProperties;
import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.auth.security.JwtAuthenticationFilter;
import com.project.klare_server.auth.service.JwtService;
import com.project.klare_server.common.idempotency.IdempotencyFilter;
import com.project.klare_server.common.idempotency.IdempotencyService;
import com.project.klare_server.common.ratelimit.RateLimitFilter;
import com.project.klare_server.common.security.RestAccessDeniedHandler;
import com.project.klare_server.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/docs/**"
    };

    private final SecurityProperties securityProperties;
    private final RateLimitProperties rateLimitProperties;
    private final IdempotencyProperties idempotencyProperties;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final IdempotencyService idempotencyService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            SecurityProperties securityProperties,
            RateLimitProperties rateLimitProperties,
            IdempotencyProperties idempotencyProperties,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            IdempotencyService idempotencyService,
            JwtService jwtService,
            ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.idempotencyProperties = idempotencyProperties;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.idempotencyService = idempotencyService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimitProperties, objectMapper);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        IdempotencyFilter idempotencyFilter = new IdempotencyFilter(idempotencyService, idempotencyProperties, objectMapper);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class)
                .addFilterAfter(idempotencyFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        SecurityProperties.Cors cors = securityProperties.cors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.allowedOrigins());
        configuration.setAllowedMethods(cors.allowedMethods());
        configuration.setAllowedHeaders(cors.allowedHeaders());
        configuration.setExposedHeaders(cors.exposedHeaders());
        configuration.setAllowCredentials(cors.allowCredentials());
        configuration.setMaxAge(cors.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
