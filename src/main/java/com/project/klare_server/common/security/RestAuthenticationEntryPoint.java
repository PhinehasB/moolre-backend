package com.project.klare_server.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.klare_server.common.error.ApiError;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        ApiError error = ApiError.of(ErrorCode.UNAUTHORIZED.name(), "Authentication required", UUID.randomUUID().toString());
        response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(error));
    }
}
