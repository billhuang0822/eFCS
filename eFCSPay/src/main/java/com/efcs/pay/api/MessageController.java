package com.efcs.pay.api;

import com.efcs.pay.service.EfcsMessageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final EfcsMessageService service;

    public MessageController(EfcsMessageService service) {
        this.service = service;
    }

    @PostMapping(value = "/{prsCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> send(@PathVariable String prsCode,
                                    @RequestBody Map<String, Object> message) {
        return service.send(prsCode, message);
    }
}
