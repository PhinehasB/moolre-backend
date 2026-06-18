package com.project.klare_server.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            String first = comma > -1 ? forwarded.substring(0, comma) : forwarded;
            String trimmed = first.trim();
            if (StringUtils.hasText(trimmed)) {
                return trimmed;
            }
        }
        return request.getRemoteAddr();
    }
}
