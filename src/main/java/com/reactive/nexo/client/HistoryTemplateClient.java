package com.reactive.nexo.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HistoryTemplateClient {
    private WebClient client = WebClient.create("http://localhost:8085");

    public Mono<String> getHealth() {
        return client.get()
                .uri("/actuator/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);
    }
}
