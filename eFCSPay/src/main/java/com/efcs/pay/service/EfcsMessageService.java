package com.efcs.pay.service;

import com.efcs.pay.dto.MessageDtos;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static com.efcs.pay.dto.MessageDtos.*;

@Service
public class EfcsMessageService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String hostUrl;

    public EfcsMessageService(RestClient restClient, ObjectMapper objectMapper,
                              @Value("${efcs.host.base-url:http://localhost:8091/mock/efcs}") String hostUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.hostUrl = hostUrl.replaceAll("/$", "");
    }

    public Envelope<?> send(String prsCode, JsonNode input) {
        Class<?> requestType = requestBodyType(prsCode);
        Class<?> responseType = responseBodyType(prsCode);
        JavaType requestEnvelopeType = envelopeType(requestType);
        Envelope<?> request = objectMapper.convertValue(input, requestEnvelopeType);
        if (request.head() == null) throw new IllegalArgumentException("HEAD is required");
        if (!prsCode.equals(request.head().prsCode())) {
            throw new IllegalArgumentException("URL prsCode must match HEAD.prsCode");
        }
        if (request.body() == null) throw new IllegalArgumentException("BODY is required for " + prsCode);

        String responseJson = restClient.post().uri(hostUrl + "/" + prsCode)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(String.class);
        try {
            return objectMapper.readValue(responseJson, envelopeType(responseType));
        } catch (Exception ex) {
            throw new IllegalStateException("Host returned an invalid " + prsCode + " response", ex);
        }
    }

    private JavaType envelopeType(Class<?> bodyType) {
        return objectMapper.getTypeFactory().constructParametricType(Envelope.class, bodyType);
    }

    private Class<?> requestBodyType(String code) {
        return switch (code) {
            case "T101" -> T101RequestBody.class;
            case "T102" -> T102RequestBody.class;
            case "T112" -> T112RequestBody.class;
            case "T201" -> T201RequestBody.class;
            default -> throw new IllegalArgumentException("Unsupported prsCode: " + code);
        };
    }

    private Class<?> responseBodyType(String code) {
        return switch (code) {
            case "T101" -> T101ResponseBody.class;
            case "T102" -> T102ResponseBody.class;
            case "T112" -> Void.class;
            case "T201" -> T201ResponseBody.class;
            default -> throw new IllegalArgumentException("Unsupported prsCode: " + code);
        };
    }
}
