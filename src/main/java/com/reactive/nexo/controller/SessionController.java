package com.reactive.nexo.controller;

import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.LoginResponse;
import com.reactive.nexo.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@Slf4j
public class SessionController {

    @Autowired
    private SessionService sessionService;

    /**
     * POST /api/v1/auth/login - Login endpoint
     * Accepts identification_type, identification_number, and password
     * Returns JWT token if successful
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        log.info("SessionController.login - Login request for user: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());
        
        return sessionService.login(request)
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
     * POST /api/v1/auth/logout - Logout endpoint (stateless, no action needed on server)
     */
    @GetMapping("/logout2")
    public Mono<ResponseEntity<Void>> logout2() {
        log.info("SessionController.logout - Logout request");
        return Mono.just(ResponseEntity.ok().<Void>build());
    }
}

