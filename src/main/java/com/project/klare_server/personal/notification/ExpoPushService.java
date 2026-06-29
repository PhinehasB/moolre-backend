package com.project.klare_server.personal.notification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExpoPushService {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushService.class);
    private static final String ENDPOINT = "https://exp.host/--/api/v2/push/send";

    private final RestClient restClient = RestClient.create();

    public void send(List<String> tokens, String title, String body, Map<String, Object> data) {
        List<Map<String, Object>> messages = tokens.stream()
                .filter(token -> token != null && token.startsWith("ExponentPushToken"))
                .map(token -> message(token, title, body, data))
                .toList();
        if (messages.isEmpty()) {
            return;
        }
        try {
            restClient.post()
                    .uri(ENDPOINT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(messages)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Expo push sent to {} device(s)", messages.size());
        } catch (Exception ex) {
            log.error("Expo push failed: {}", ex.toString());
        }
    }

    private Map<String, Object> message(String token, String title, String body, Map<String, Object> data) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("to", token);
        message.put("title", title);
        message.put("body", body);
        message.put("sound", "default");
        message.put("priority", "high");
        if (data != null) {
            message.put("data", data);
        }
        return message;
    }
}
