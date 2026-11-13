package com.efcs.pay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashSet;
import java.util.Set;

/** Relays messages and transparently follows T101/T102 continuation keys. */
@Service
public class EfcsMessageService {
    private static final Set<String> SUPPORTED_CODES = Set.of("T101", "T102", "T112", "T201");
    private static final String END_KEY = "END";
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String hostUrl;
    private final int maxPages;

    public EfcsMessageService(RestClient restClient, ObjectMapper objectMapper,
                              @Value("${efcs.host.base-url:http://localhost:8091/mock/efcs}") String hostUrl,
                              @Value("${efcs.pagination.max-pages:20}") int maxPages) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.hostUrl = hostUrl.replaceAll("/$", "");
        if (maxPages < 1) throw new IllegalArgumentException("efcs.pagination.max-pages must be >= 1");
        this.maxPages = maxPages;
    }

    public JsonNode send(String prsCode, JsonNode input) {
        if (!SUPPORTED_CODES.contains(prsCode)) throw new IllegalArgumentException("Unsupported prsCode: " + prsCode);
        if (input == null || !input.isObject()) throw new IllegalArgumentException("Message must be a JSON object");
        JsonNode head = input.path("HEAD");
        if (!head.isObject()) throw new IllegalArgumentException("HEAD must be a JSON object");
        if (!prsCode.equals(head.path("prsCode").asText())) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        if (!input.path("BODY").isObject()) throw new IllegalArgumentException("BODY must be a JSON object");
        if (prsCode.equals("T101") || prsCode.equals("T102")) {
            return exchangePages(prsCode, (ObjectNode) input.deepCopy());
        }
        return exchange(prsCode, input);
    }

    private JsonNode exchangePages(String code, ObjectNode request) {
        String detailName = code.equals("T101") ? "QUERYDETAIL" : "PAYDETAIL";
        ArrayNode allDetails = objectMapper.createArrayNode();
        ObjectNode accumulatedBody = null;
        Set<String> returnedKeys = new HashSet<>();
        String requestKey = request.path("BODY").path("key").asText("");

        for (int page = 1; page <= maxPages; page++) {
            ((ObjectNode) request.get("BODY")).put("key", requestKey);
            JsonNode response = exchange(code, request);
            JsonNode bodyNode = response.path("BODY");
            if (!bodyNode.isObject()) throw new IllegalStateException("Host response BODY is missing for " + code);
            JsonNode details = bodyNode.path(detailName);
            if (!details.isArray()) throw new IllegalStateException("Host response BODY." + detailName + " must be an array");

            details.forEach(allDetails::add);
            if (accumulatedBody == null) accumulatedBody = (ObjectNode) bodyNode.deepCopy();
            accumulatedBody.set(detailName, allDetails);

            String nextKey = bodyNode.path("key").asText("");
            // Per protocol, END—not an empty string—is the terminal cursor.
            if (END_KEY.equals(nextKey)) {
                accumulatedBody.put("key", END_KEY);
                ObjectNode result = response.deepCopy();
                result.set("BODY", accumulatedBody);
                return result;
            }
            if (nextKey.isBlank()) {
                throw new IllegalStateException("Host returned an empty continuation key; expected END or a next-page key for " + code);
            }
            if (nextKey.equals(requestKey) || !returnedKeys.add(nextKey)) {
                throw new IllegalStateException("Host returned a repeated continuation key for " + code);
            }
            if (page == maxPages) throw new IllegalStateException("Maximum page limit reached for " + code);
            requestKey = nextKey;
        }
        throw new IllegalStateException("Pagination ended unexpectedly for " + code);
    }

    private JsonNode exchange(String code, JsonNode request) {
        String response = restClient.post().uri(hostUrl + "/" + code)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(String.class);
        try {
            JsonNode json = objectMapper.readTree(response);
            if (json == null || !json.path("HEAD").isObject()) {
                throw new IllegalStateException("Host response HEAD is missing");
            }
            return json;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Host returned invalid JSON for " + code, ex);
        }
    }
}
