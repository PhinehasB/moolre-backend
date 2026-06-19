package com.project.klare_server.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.config.properties.IdempotencyProperties;
import com.project.klare_server.common.error.ApiError;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.web.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";
    private static final String API_PATH_PREFIX = "/api/";
    private static final String ANONYMOUS_SCOPE = "anonymous";
    private static final int MAX_KEY_LENGTH = 255;
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, IdempotencyProperties properties, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled()
                || !MUTATING_METHODS.contains(request.getMethod())
                || !request.getRequestURI().startsWith(API_PATH_PREFIX)
                || !StringUtils.hasText(request.getHeader(IDEMPOTENCY_KEY_HEADER));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER).trim();
        if (key.length() > MAX_KEY_LENGTH) {
            writeError(response, ErrorCode.VALIDATION_ERROR, "Idempotency-Key exceeds maximum length");
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String scope = resolveScope();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String requestHash = idempotencyService.hashRequest(method, path, cachedRequest.getCachedBody());

        IdempotencyOutcome outcome = idempotencyService.begin(scope, key, method, path, requestHash);
        switch (outcome.state()) {
            case REPLAY -> writeReplay(response, outcome);
            case IN_PROGRESS -> writeError(response, ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                    "A request with this Idempotency-Key is still being processed");
            case MISMATCH -> writeError(response, ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                    "Idempotency-Key was already used with a different request");
            case STARTED -> proceedAndCapture(cachedRequest, response, chain, outcome.recordId());
        }
    }

    private void proceedAndCapture(CachedBodyHttpServletRequest request, HttpServletResponse response,
                                   FilterChain chain, UUID recordId) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        boolean released = false;
        try {
            chain.doFilter(request, wrapper);
        } catch (RuntimeException | ServletException | IOException ex) {
            idempotencyService.release(recordId);
            released = true;
            throw ex;
        } finally {
            if (!released) {
                int status = wrapper.getStatus();
                byte[] body = wrapper.getContentAsByteArray();
                if (status >= 500) {
                    idempotencyService.release(recordId);
                } else {
                    idempotencyService.complete(recordId, status, response.getContentType(), body);
                }
            }
            wrapper.copyBodyToResponse();
        }
    }

    private void writeReplay(HttpServletResponse response, IdempotencyOutcome outcome) throws IOException {
        response.setStatus(outcome.responseStatus() != null ? outcome.responseStatus() : HttpServletResponse.SC_OK);
        if (StringUtils.hasText(outcome.responseContentType())) {
            response.setContentType(outcome.responseContentType());
        }
        response.setHeader(IDEMPOTENCY_REPLAYED_HEADER, "true");
        if (outcome.responseBody() != null && outcome.responseBody().length > 0) {
            response.getOutputStream().write(outcome.responseBody());
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(code.name(), message, UUID.randomUUID().toString());
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(error));
    }

    private String resolveScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id().toString();
        }
        return ANONYMOUS_SCOPE;
    }
}
