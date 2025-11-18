package com.reactive.nexo.client;

import com.reactive.nexo.dto.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.reactive.nexo.model.Session;

@Component
@Slf4j
public class SessionClient {
    private WebClient client = WebClient.create("http://localhost:8080");

    /**
     * Call the employees module to authenticate a user
     * POST /api/v1/employees/authenticate
     */
    public Mono<com.reactive.nexo.dto.AuthResponse> authenticate(LoginRequest request) {
        log.info("EmployeeClient.authenticate - Calling employees module for authentication");
        return client.post()
                .uri("/api/v1/employees/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(com.reactive.nexo.dto.AuthResponse.class)
                .doOnSuccess(resp -> log.info("EmployeeClient.authenticate - Auth successful for user: {}", resp.getId()))
                .doOnError(err -> log.error("EmployeeClient.authenticate - Auth failed: {}", err.getMessage()));
    }
    
    public Mono<Session> getTest(){
       return client.get()
                .uri("/test")
                .retrieve()
                .bodyToMono(Session.class).log(" Employee fetched ");
    }
}
