package com.rajneeti.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration.
 *
 * <p>Strategy:
 * <ul>
 *   <li>Stateless (no HTTP sessions) – JWT carries all auth state
 *   <li>CSRF disabled – not needed for stateless REST/JWT APIs
 *   <li>CORS uses the {@link CorsConfigurationSource} bean from {@link com.rajneeti.config.CorsConfig}
 *   <li>Public routes: health check, Swagger, WebSocket handshake
 *   <li>Everything else requires a valid Bearer token
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter    jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService          userDetailsService;
    private final CorsConfigurationSource     corsConfigurationSource;

    // ──────────────────────────────────────────────────────────────────────────
    // Public route patterns
    // ──────────────────────────────────────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/api/health",
            "/api/v1/auth/**",
            "/ws/**",               // WebSocket handshake
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",      // OpenAPI (future)
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CORS ──────────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ── CSRF: disabled for stateless REST/JWT ─────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── Session: stateless ────────────────────────────────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── 401 handler ───────────────────────────────────────────
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // ── Route authorization ───────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())

                // ── JWT filter ────────────────────────────────────────────
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
