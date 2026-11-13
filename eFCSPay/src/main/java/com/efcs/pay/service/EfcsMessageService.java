package com.efcs.pay.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

@Service
public class EfcsMessageService {
    private static final Set<String> SUPPORTED_CODES = Set.of("T101", "T102", "T112", "T201");

    private final RestClient restClient;
    private final String hostUrl;

    public EfcsMessageService(RestClient restClient,
                              @Value("${efcs.host.base-url:http://localhost:8091/mock/efcs}") String hostUrl) {
        this.restClient = restClient;
        this.hostUrl = hostUrl;
    }

    public Map<String, Object> send(String prsCode, Map<String, Object> message) {
        if (!SUPPORTED_CODES.contains(prsCode)) {
            throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        }
        Object headValue = message.get("HEAD");
        if (!(headValue instanceof Map<?, ?> head)) {
            throw new IllegalArgumentException("HEAD must be a JSON object");
        }
        if (!prsCode.equals(head.get("prsCode"))) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        return restClient.post()
                .uri(hostUrl + "/" + prsCode)
                .body(message)
                .retrieve()
                .body(Map.class);
    }
}
