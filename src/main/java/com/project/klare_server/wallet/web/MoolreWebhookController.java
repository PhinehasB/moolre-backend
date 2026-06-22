package com.project.klare_server.wallet.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.klare_server.moolre.MoolreProperties;
import com.project.klare_server.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/moolre")
@Tag(name = "Moolre Webhook", description = "Receives payment callbacks from Moolre")
@SecurityRequirements
public class MoolreWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MoolreWebhookController.class);

    private final WalletService walletService;
    private final MoolreProperties properties;

    public MoolreWebhookController(WalletService walletService, MoolreProperties properties) {
        this.walletService = walletService;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody JsonNode payload) {
        JsonNode data = payload.path("data");
        String secret = data.path("secret").asText(null);
        String expected = properties.active().callbackSecret();

        if (!StringUtils.hasText(expected) || !expected.equals(secret)) {
            log.warn("Rejected Moolre webhook with invalid secret externalref={}", data.path("externalref").asText(null));
            return ResponseEntity.ok().build();
        }

        String externalRef = data.path("externalref").asText(null);
        String transactionId = data.path("transactionid").asText(null);
        int txstatus = data.path("txstatus").asInt(0);
        String accountNumber = data.path("accountnumber").asText(null);
        java.math.BigDecimal amount = data.has("value") && !data.path("value").asText("").isBlank()
                ? new java.math.BigDecimal(data.path("value").asText())
                : (data.has("amount") && !data.path("amount").asText("").isBlank()
                        ? new java.math.BigDecimal(data.path("amount").asText()) : null);
        walletService.creditFromWebhook(externalRef, transactionId, txstatus, accountNumber, amount);
        return ResponseEntity.ok().build();
    }
}
