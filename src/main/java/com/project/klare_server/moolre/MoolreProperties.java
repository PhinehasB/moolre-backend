package com.project.klare_server.moolre;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "klare.moolre")
public record MoolreProperties(
        @DefaultValue("sandbox") String env,
        String callbackUrl,
        @DefaultValue("GHS") String currency,
        @DefaultValue("Klare") String smsSenderId,
        Credentials sandbox,
        Credentials live) {

    public record Credentials(
            String baseUrl,
            String apiUser,
            String apiKey,
            String apiPubkey,
            String accountNumber,
            String callbackSecret,
            String vasKey) {
    }

    public Credentials active() {
        return "live".equalsIgnoreCase(env) ? live : sandbox;
    }
}
