package com.efcs.pay.api;

import com.efcs.pay.service.EfcsMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final EfcsMessageService service;

    public MessageController(EfcsMessageService service) { this.service = service; }

    @PostMapping(value = "/{prsCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode send(@PathVariable String prsCode, @RequestBody JsonNode message) {
        return service.send(prsCode, message);
    }
}
