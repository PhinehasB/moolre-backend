package com.project.klare_server.common.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "klare.idempotency")
public record IdempotencyProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("PT24H") Duration ttl) {
}
