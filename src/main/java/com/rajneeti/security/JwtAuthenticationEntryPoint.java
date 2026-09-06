package com.rajneeti.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom {@link AuthenticationEntryPoint} invoked when an unauthenticated request
 * hits a protected endpoint.
 *
 * <p>Returns a JSON 401 response instead of the default Spring redirect to a login page.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("Unauthorized request to [{}]: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {
                  "status": 401,
                  "error": "UNAUTHORIZED",
                  "message": "Authentication required. Please provide a valid Bearer token.",
                  "path": "%s"
                }
                """.formatted(request.getRequestURI()));
    }
}
