package com.project.klare_server.moolre;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MoolreClient {

    private static final Logger log = LoggerFactory.getLogger(MoolreClient.class);

    private final MoolreProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MoolreClient(MoolreProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .baseUrl(properties.active().baseUrl())
                .requestFactory(factory)
                .build();
    }

    public AccountStatus accountStatus() {
        Map<String, Object> body = base();
        JsonNode node = postPrivate("/open/account/status", body);
        JsonNode data = node.path("data");
        return new AccountStatus(
                new BigDecimal(data.path("balance").asText("0")),
                data.path("accountname").asText(null));
    }

    public String validateName(String channel, String receiver) {
        Map<String, Object> body = base();
        body.put("channel", channel);
        body.put("receiver", receiver);
        JsonNode node = postPrivate("/open/transact/validate", body);
        if (node.path("status").asInt(0) != 1) {
            throw new MoolreException(node.path("code").asText("AVD02"), text(node, "Account name could not be validated"));
        }
        return node.path("data").asText(null);
    }

    public TransferResult transfer(String channel, BigDecimal amount, String receiver, String externalref, String reference) {
        Map<String, Object> body = base();
        body.put("channel", channel);
        body.put("amount", amount.toPlainString());
        body.put("receiver", receiver);
        body.put("externalref", externalref);
        if (reference != null) {
            body.put("reference", reference);
        }
        JsonNode node = postPrivate("/open/transact/transfer", body);
        JsonNode data = node.path("data");
        Integer txstatus = data.has("txstatus") ? data.get("txstatus").asInt() : null;
        return new TransferResult(
                txstatus,
                node.path("code").asText(null),
                text(node, null),
                data.path("transactionid").asText(null),
                data.path("receivername").asText(null));
    }

    public TransferStatusResult transferStatus(String externalref) {
        Map<String, Object> body = base();
        body.put("idtype", 1);
        body.put("id", externalref);
        JsonNode node = postPrivate("/open/transact/status", body);
        JsonNode data = node.path("data");
        Integer txstatus = data.has("txstatus") ? data.get("txstatus").asInt() : null;
        return new TransferStatusResult(txstatus, data.path("transactionid").asText(null), node.path("code").asText(null));
    }

    public PaymentResult initiatePayment(String channel, String payer, BigDecimal amount, String externalref,
                                         String otpcode, String sessionid, String reference) {
        Map<String, Object> body = base();
        body.put("channel", channel);
        body.put("payer", payer);
        body.put("amount", amount.toPlainString());
        body.put("externalref", externalref);
        if (otpcode != null) {
            body.put("otpcode", otpcode);
        }
        if (sessionid != null) {
            body.put("sessionid", sessionid);
        }
        if (reference != null) {
            body.put("reference", reference);
        }
        JsonNode node = postPublic("/open/transact/payment", body);
        return new PaymentResult(
                node.path("status").asInt(0),
                node.path("code").asText(null),
                text(node, null),
                node.path("data").asText(null));
    }

    private Map<String, Object> base() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", 1);
        body.put("currency", properties.currency());
        body.put("accountnumber", properties.active().accountNumber());
        return body;
    }

    private JsonNode postPrivate(String path, Map<String, Object> body) {
        return exchange(path, body, "X-API-KEY", properties.active().apiKey());
    }

    private JsonNode postPublic(String path, Map<String, Object> body) {
        return exchange(path, body, "X-API-PUBKEY", properties.active().apiPubkey());
    }

    private JsonNode exchange(String path, Map<String, Object> body, String keyHeader, String keyValue) {
        try {
            return restClient.post()
                    .uri(path)
                    .header("X-API-USER", properties.active().apiUser())
                    .header(keyHeader, keyValue)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .exchange((request, response) -> objectMapper.readTree(response.getBody()));
        } catch (Exception ex) {
            log.error("Moolre call failed path={} error={}", path, ex.toString());
            throw new MoolreException("MOOLRE_UNREACHABLE", "Could not reach the payment gateway");
        }
    }

    private String text(JsonNode node, String fallback) {
        JsonNode message = node.path("message");
        if (message.isArray() && !message.isEmpty()) {
            return message.get(0).asText(fallback);
        }
        if (message.isTextual()) {
            return message.asText(fallback);
        }
        return fallback;
    }

    public record AccountStatus(BigDecimal balance, String accountName) {
    }

    public record TransferResult(Integer txstatus, String code, String message, String transactionId, String receiverName) {
    }

    public record TransferStatusResult(Integer txstatus, String transactionId, String code) {
    }

    public record PaymentResult(int status, String code, String message, String data) {
    }
}
