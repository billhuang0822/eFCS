package com.efcs.utility.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Initial blocking client for T101/T102/T112/T201. T101/T102 host cursors are
 * followed until the host returns BODY.key=END; page details are aggregated.
 * No automatic retries are performed because payment requests must not be replayed blindly.
 */
@Component
public class EfcsMessageClient {
    private static final Set<String> CODES = Set.of("T101", "T102", "T112", "T201");
    private static final String END = "END";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final int maxPages;

    public EfcsMessageClient(RestClient.Builder builder, JsonMapper jsonMapper,
                             @Value("${efcs.host.base-url:http://localhost:8091/mock/efcs}") String baseUrl,
                             @Value("${efcs.pagination.max-pages:20}") int maxPages) {
        if (maxPages < 1) throw new IllegalArgumentException("efcs.pagination.max-pages must be >= 1");
        this.restClient = builder.build();
        this.jsonMapper = jsonMapper;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.maxPages = maxPages;
    }

    private final String baseUrl;

    public JsonNode send(String prsCode, JsonNode request) {
        validateRequest(prsCode, request);
        if (prsCode.equals("T101") || prsCode.equals("T102")) {
            return sendPaged(prsCode, (ObjectNode) request.deepCopy());
        }
        return exchange(prsCode, request);
    }

    private void validateRequest(String prsCode, JsonNode request) {
        if (!CODES.contains(prsCode)) throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        if (request == null || !request.isObject()) throw new IllegalArgumentException("Request must be a JSON object");
        if (!request.path("HEAD").isObject()) throw new IllegalArgumentException("HEAD must be a JSON object");
        if (!prsCode.equals(request.path("HEAD").path("prsCode").asText())) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        if (!request.path("BODY").isObject()) throw new IllegalArgumentException("BODY must be a JSON object");
    }

    private JsonNode sendPaged(String code, ObjectNode request) {
        String detailName = code.equals("T101") ? "QUERYDETAIL" : "PAYDETAIL";
        ArrayNode allDetails = jsonMapper.createArrayNode();
        ObjectNode collectedBody = null;
        Set<String> seenKeys = new HashSet<>();
        String cursor = request.path("BODY").path("key").asText("");

        for (int page = 1; page <= maxPages; page++) {
            ((ObjectNode) request.get("BODY")).put("key", cursor);
            JsonNode response = exchange(code, request);
            JsonNode body = response.path("BODY");
            if (!body.isObject()) throw new IllegalStateException("Host response BODY is missing for " + code);
            JsonNode details = body.path(detailName);
            if (!details.isArray()) throw new IllegalStateException("Host response BODY." + detailName + " must be an array");
            details.forEach(allDetails::add);
            if (collectedBody == null) collectedBody = (ObjectNode) body.deepCopy();
            collectedBody.set(detailName, allDetails);

            String nextKey = body.path("key").asText("");
            if (END.equals(nextKey)) {
                collectedBody.put("key", END);
                ObjectNode result = response.deepCopy();
                result.set("BODY", collectedBody);
                return result;
            }
            if (nextKey.isBlank()) throw new IllegalStateException("Host returned empty key; expected END or continuation key");
            if (nextKey.equals(cursor) || !seenKeys.add(nextKey)) {
                throw new IllegalStateException("Host returned repeated continuation key for " + code);
            }
            if (page == maxPages) throw new IllegalStateException("Pagination exceeded configured page limit " + maxPages);
            cursor = nextKey;
        }
        throw new IllegalStateException("Pagination ended unexpectedly");
    }

    private JsonNode exchange(String code, JsonNode request) {
        JsonNode response = restClient.post()
                .uri(baseUrl + "/" + code)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.path("HEAD").isObject()) {
            throw new IllegalStateException("Host response HEAD is missing for " + code);
        }
        if (!code.equals(response.path("HEAD").path("prsCode").asText())) {
            throw new IllegalStateException("Host response HEAD.prsCode does not match " + code);
        }
        return response;
    }
}
