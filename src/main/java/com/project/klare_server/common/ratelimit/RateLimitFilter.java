package com.project.klare_server.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.klare_server.common.config.properties.RateLimitProperties;
import com.project.klare_server.common.error.ApiError;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.common.web.ClientIpResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth";
    private static final String API_PATH_PREFIX = "/api/";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith(API_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean authScope = request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
        String scope = authScope ? "auth" : "global";
        RateLimitProperties.Bucket config = authScope ? properties.auth() : properties.global();

        String key = scope + ":" + ClientIpResolver.resolve(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket(config));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", Long.toString(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setHeader("X-Rate-Limit-Retry-After-Seconds", Long.toString(retryAfterSeconds));
        response.setStatus(ErrorCode.RATE_LIMITED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(ErrorCode.RATE_LIMITED.name(), "Too many requests, please retry later", UUID.randomUUID().toString());
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(error));
    }

    private Bucket newBucket(RateLimitProperties.Bucket config) {
        Duration period = config.refillPeriod() == null ? Duration.ofMinutes(1) : config.refillPeriod();
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.capacity())
                .refillGreedy(config.refillTokens(), period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
