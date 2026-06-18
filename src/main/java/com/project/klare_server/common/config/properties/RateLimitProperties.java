package com.project.klare_server.common.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "klare.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        Bucket auth,
        Bucket global) {

    public record Bucket(
            @DefaultValue("60") long capacity,
            @DefaultValue("60") long refillTokens,
            @DefaultValue("PT1M") Duration refillPeriod) {
    }
}
