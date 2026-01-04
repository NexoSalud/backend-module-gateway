package com.reactive.nexo.controller;

import org.springframework.web.server.ServerWebExchange;
import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.LoginResponse;
import com.reactive.nexo.dto.ChangePasswordRequest;
import com.reactive.nexo.dto.UpdatePasswordRequest;
import com.reactive.nexo.service.SessionService;
import com.reactive.nexo.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.jsonwebtoken.Claims;
import com.reactive.nexo.dto.ResetPasswordRequest;
import com.reactive.nexo.dto.ResetPasswordResponse;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.reactive.nexo.util.TwoFactorUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/auth")
@Slf4j
public class SessionController {

    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    /**
     * POST /api/v1/auth/login - Login endpoint
     * Accepts identification_type, identification_number, and password
     * Returns JWT token if successful
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request, ServerWebExchange exchange) {
        log.info("SessionController.login - Login request for user: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());
        
        return sessionService.login(request,exchange)
                .map(response -> ResponseEntity.status(HttpStatus.OK).body(response))
                .onErrorResume(err -> {
                    log.error("SessionController.login - Login failed: {}", err.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * POST /api/v1/auth/logout - Logout endpoint (stateless, no action needed on server)
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout() {
        log.info("SessionController.logout - Logout request");
        return Mono.just(ResponseEntity.ok().<Void>build());
    }

   /**
    * POST /api/v1/auth/reset-password - Reset password bootstrap
    * Accepts identificationType and identificationNumber and echoes back the same.
    */
    @PostMapping("/reset-password")
    public Mono<ResponseEntity<Boolean>> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("SessionController.resetPassword - Request for user: {}/{}",
                request.getIdentificationType(), request.getIdentificationNumber());

        return sessionService
                .resetPassword(request.getIdentificationType(), request.getIdentificationNumber())
                .map(success -> ResponseEntity.ok(success))
                .onErrorResume(err -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false)));
    }

    /**
     * POST /api/v1/auth/change-password/{token} - Change password endpoint
     * Extracts employee_id from token and validates it against the request payload
     * Then makes a PATCH request to employees module to update the password
     */
    @PostMapping("/change-password/{token}")
    public Mono<ResponseEntity<String>> changePassword(
            @PathVariable String token,
            @RequestBody ChangePasswordRequest request) {
        
        log.info("SessionController.changePassword - Change password request for token: {}", 
                token.substring(0, Math.min(token.length(), 20)) + "...");
        
        try {
            // Extract claims from token
            Claims claims = jwtUtil.extractClaims(token);
            if (claims == null) {
                log.warn("SessionController.changePassword - Invalid token");
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token inválido"));
            }
            
            // Extract employee_id from token
            String employeeEmail = (String) claims.get("employee_email");
            String employeeId = (String) claims.get("employee_id");
            if (employeeEmail == null) {
                log.warn("SessionController.changePassword - No employee_email found in token");
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token no contiene employee_email"));
            }
            
            // Validate employee_id from token matches the one in request
            if (!employeeEmail.equals(request.getEmployee_email())) {
                log.warn("SessionController.changePassword - Employee ID mismatch. Token: {}, Request: {}", 
                        employeeEmail, request.getEmployee_email());
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("El employee_id del token no coincide con el de la solicitud"));
            }
            
            // Create update password request
            UpdatePasswordRequest updateRequest = new UpdatePasswordRequest();
            updateRequest.setPassword(request.getNew_password());
            
            // Make PATCH request to employees module
            //@Value("${service.employees.url}")
            String employeesUrl = "http://localhost:8081"; // URL del módulo employees
            WebClient webClient = webClientBuilder.baseUrl(employeesUrl).build();
            
            return webClient.patch()
                    .uri("/api/v1/employees/{id}", employeeId)
                    .bodyValue(updateRequest)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            log.info("SessionController.changePassword - Password updated successfully for employee: {}", 
                                    employeeId);
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("Contraseña actualizada exitosamente");
                        } else {
                            log.error("SessionController.changePassword - Failed to update password for employee: {}. Status: {}", 
                                    employeeId, response.statusCode());
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("Error al actualizar la contraseña")
                                    .map(body -> ResponseEntity.status(response.statusCode()).body(body))
                                    .cast(ResponseEntity.class)
                                    .map(ResponseEntity::getBody)
                                    .cast(String.class);
                        }
                    })
                    .map(body -> ResponseEntity.ok(body))
                    .onErrorResume(error -> {
                        log.error("SessionController.changePassword - Error communicating with employees module: {}", 
                                error.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error de comunicación con el servicio de empleados"));
                    });
                    
        } catch (Exception e) {
            log.error("SessionController.changePassword - Unexpected error: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor"));
        }
    }
}

