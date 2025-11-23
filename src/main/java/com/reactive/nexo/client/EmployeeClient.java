package com.reactive.nexo.client;

import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.Arrays;

@Component
@Slf4j
public class EmployeeClient {
    private WebClient client = WebClient.create("http://localhost:8081");

    @Value("${auth.mock-mode:false}")
    private boolean mockMode;

    /**
     * Call the employees module to authenticate a user
     * POST /api/v1/employees/authenticate
     * If mock-mode is enabled, returns a mock response without calling the remote service
     */
    public Mono<AuthResponse> authenticate(LoginRequest request) {
        log.info("EmployeeClient.authenticate - Attempting authentication for user: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());
        
        return client.post()
                .uri("/api/v1/employees/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .doOnSuccess(resp -> log.info("EmployeeClient.authenticate - Auth successful for user id: {}", resp.getId()))
                .doOnError(err -> {
                    if (err instanceof WebClientResponseException) {
                        log.error("EmployeeClient.authenticate - HTTP error: {}", ((WebClientResponseException) err).getStatusCode());
                    } else {
                        log.error("EmployeeClient.authenticate - Auth failed: {}", err.getMessage());
                    }
                });
    }
}
