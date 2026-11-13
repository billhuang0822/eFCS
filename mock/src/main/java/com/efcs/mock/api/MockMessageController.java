package com.efcs.mock.api;

import com.efcs.mock.service.MockMessageService;
import com.efcs.pay.dto.MessageDtos.Envelope;
import tools.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mock/efcs")
public class MockMessageController {
    private final MockMessageService service;
    public MockMessageController(MockMessageService service) { this.service = service; }
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP", "service", "efcs-mock-host"); }
    @PostMapping(value = "/{prsCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Envelope<?> handle(@PathVariable String prsCode, @RequestBody JsonNode request) {
        return service.respond(prsCode, request);
    }
}
