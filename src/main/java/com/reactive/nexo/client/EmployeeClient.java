package com.reactive.nexo.client;

import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EmployeeClient {

    private final WebClient client;

    public EmployeeClient() {
        String url = System.getenv().getOrDefault("EMPLOYEES_SERVICE_URL", "http://localhost:8081");
        this.client = WebClient.create(url);
        log.info("EmployeeClient initialized with URL: {}", url);
    }

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
                .uri("/api/v1/employees/by-identification/{type}/{number}", identificationType, identificationNumber)
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .doOnSuccess(resp -> log.info("EmployeeClient.getEmployee - Retrieved employee id: {}", resp.getId()))
                .doOnError(err -> log.error("EmployeeClient.getEmployee - Failed: {}", err.getMessage()));
    }

    public Mono<Boolean> updateTwoFactorSecret(String employeeId, String newSecret) {
        return client.patch()
                .uri("/api/v1/employees/{id}", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ \"secret\": \"" + newSecret + "\" }")
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("EmployeeClient.updateTwoFactorSecret - 2FA updated for employeeId: {}", employeeId))
                .doOnError(err -> log.error("EmployeeClient.updateTwoFactorSecret - Failed for employeeId {}: {}", employeeId, err.getMessage()))
                .thenReturn(true)
                .onErrorReturn(false);
    }

    public Mono<Boolean> resetPassword(String identificationType, String identificationNumber) {
        return client.get()
                .uri("/api/v1/employees/reset-password/{type}/{number}", identificationType, identificationNumber)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(v -> log.info("EmployeeClient.resetPassword - Password reset for: {}/{}", identificationType, identificationNumber))
                .doOnError(err -> log.error("EmployeeClient.resetPassword - Failed for {}/{}: {}", identificationType, identificationNumber, err.getMessage()))
                .thenReturn(true)
                .onErrorReturn(false);
    }
}
