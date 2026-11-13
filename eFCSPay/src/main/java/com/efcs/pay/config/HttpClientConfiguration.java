package com.efcs.pay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {
    @Bean
    RestClient efcsRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
