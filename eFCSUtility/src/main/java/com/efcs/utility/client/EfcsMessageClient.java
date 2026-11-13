package com.efcs.utility.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Set;

/** Server-side host adapter. It proxies one host page per call; browser advances BODY.key. */
@Component
public class EfcsMessageClient {
    private static final Set<String> CODES = Set.of("T101", "T102", "T112", "T201");
    private final RestClient restClient;
    private final String baseUrl;

    public EfcsMessageClient(RestClient.Builder builder,
                             @Value("${efcs.host.base-url:http://localhost:8091/mock/efcs}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    public JsonNode send(String prsCode, JsonNode request) {
        if (!CODES.contains(prsCode)) throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        if (request == null || !request.isObject() || !request.path("HEAD").isObject()
                || !request.path("BODY").isObject()) {
            throw new IllegalArgumentException("Request must contain object HEAD and BODY");
        }
        if (!prsCode.equals(request.path("HEAD").path("prsCode").asText())) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        JsonNode response = restClient.post().uri(baseUrl + "/" + prsCode)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(JsonNode.class);
        if (response == null || !response.path("HEAD").isObject()
                || !prsCode.equals(response.path("HEAD").path("prsCode").asText())) {
            throw new IllegalStateException("Missing or mismatched host response HEAD for " + prsCode);
        }
        if (prsCode.equals("T101") || prsCode.equals("T102")) {
            String detailName = prsCode.equals("T101") ? "QUERYDETAIL" : "PAYDETAIL";
            if (!response.path("BODY").path(detailName).isArray()
                    || response.path("BODY").path("key").asText("").isBlank()) {
                throw new IllegalStateException("Host page must contain BODY." + detailName + " and continuation key");
            }
        }
        return response;
    }
}
