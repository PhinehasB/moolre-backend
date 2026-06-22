package com.project.klare_server.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "klare.app")
public record AppProperties(
        @DefaultValue("http://localhost:3000/reset-password") String passwordResetUrl,
        @DefaultValue("http://localhost:8080") String apiBaseUrl,
        @DefaultValue("http://localhost:3000/login") String loginUrl) {
}
