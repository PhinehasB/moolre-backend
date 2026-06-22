package com.project.klare_server.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "klare.mail")
public record MailProperties(
        @DefaultValue("no-reply@klare.app") String fromAddress,
        @DefaultValue("Klare") String fromName) {
}
