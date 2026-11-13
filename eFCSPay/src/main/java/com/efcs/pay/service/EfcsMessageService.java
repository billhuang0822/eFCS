package com.efcs.pay.service;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Set;

/** Proxies exactly one host page per browser request; the browser owns the T101/T102 cursor loop. */
@Service
public class EfcsMessageService {
    private static final Set<String> SUPPORTED_CODES = Set.of("T101", "T102", "T112", "T201");
    private final RestClient restClient;
    private final String hostUrl;

    public EfcsMessageService(RestClient restClient,
                              @Value("${efcs.host.base-url:http://localhost:8091/mock/efcs}") String hostUrl) {
        this.restClient = restClient;
        this.hostUrl = hostUrl.replaceAll("/$", "");
    }

    public JsonNode send(String prsCode, JsonNode input) {
        if (!SUPPORTED_CODES.contains(prsCode)) throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        if (input == null || !input.isObject()) throw new IllegalArgumentException("Message must be a JSON object");
        if (!input.path("HEAD").isObject()) throw new IllegalArgumentException("HEAD must be a JSON object");
        if (!prsCode.equals(input.path("HEAD").path("prsCode").asText())) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        if (!input.path("BODY").isObject()) throw new IllegalArgumentException("BODY must be a JSON object");

        // One browser request makes one upstream host call. Never follow cursors here.
        JsonNode response = restClient.post().uri(hostUrl + "/" + prsCode)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(input).retrieve().body(JsonNode.class);
        if (response == null || !response.path("HEAD").isObject()) {
            throw new IllegalStateException("Host response HEAD is missing for " + prsCode);
        }
        if (!prsCode.equals(response.path("HEAD").path("prsCode").asText())) {
            throw new IllegalStateException("Host response HEAD.prsCode does not match " + prsCode);
        }
        if (prsCode.equals("T101") || prsCode.equals("T102")) {
            String detailName = prsCode.equals("T101") ? "QUERYDETAIL" : "PAYDETAIL";
            if (!response.path("BODY").isObject() || !response.path("BODY").path(detailName).isArray()) {
                throw new IllegalStateException("Host response BODY." + detailName + " must be an array");
            }
            String nextKey = response.path("BODY").path("key").asText("");
            if (nextKey.isBlank()) {
                throw new IllegalStateException("Host response continuation key is empty; expected END or next key");
            }
        }
        return response;
    }
}
