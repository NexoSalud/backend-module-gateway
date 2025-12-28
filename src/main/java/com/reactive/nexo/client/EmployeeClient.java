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

    /**
     * Call the employees module to authenticate a user
     * POST /api/v1/employees/authenticate
     * If mock-mode is enabled, returns a mock response without calling the remote service
     */
    public Mono<AuthResponse> authenticate(LoginRequest request) {
        log.info("EmployeeClient.authenticate - Attempting authentication for user: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());
        
        java.util.Map<String, String> payload = java.util.Map.of(
            "identification_type", request.getIdentification_type(),
            "identification_number", request.getIdentification_number(),
            "password", request.getPassword()
        );

        return client.post()
                .uri("/api/v1/employees/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
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
    public Mono<AuthResponse> getEmployee(String identificationType, String identificationNumber) {
        return client.get()
                .uri("/api/v1/employees/by-identification/{identificationType}/{identificationNumber}", identificationType, identificationNumber)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .doOnSuccess(resp -> log.info("EmployeeClient.getEmployee - Retrieved employee with id: XXX#{}", resp.getId()))
                .doOnError(err -> log.error("EmployeeClient.getEmployee - Failed to update 2FA secret for employeeIdentification: {}: {}", identificationNumber, err.getMessage()));
    }   
    public Mono<Boolean> updateTwoFactorSecret(String employeeId, String newSecret) {
        return client.patch()
                .uri("/api/v1/employees/{id}", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ \"secret\": \"" + newSecret + "\" }")
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("EmployeeClient.updateTwoFactorSecret - 2FA secret updated for employeeId: {}", employeeId))
                .doOnError(err -> log.error("EmployeeClient.updateTwoFactorSecret - Failed to update 2FA secret for employeeId: {}: {}", employeeId, err.getMessage()))
                .thenReturn(true)
                .onErrorReturn(false);
    }   
}
