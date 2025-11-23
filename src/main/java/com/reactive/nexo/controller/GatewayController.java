package com.reactive.nexo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import com.reactive.nexo.security.PermissionChecker;

@RestController
public class GatewayController {

    private final WebClient webClient;
    private final String TARGET_URL = "http://localhost:8081";

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(TARGET_URL).build();
    }

    /**
     * Forwards any POST request arriving at the root path "/" 
     * to the target service at http://localhost:8081/
     */
    @PostMapping("/**") // Maps all POST paths dynamically
    public Mono<ResponseEntity<String>> forwardPostRequests(ServerWebExchange exchange) {
   
        // Dynamically determine the target path (e.g., /auth/login)
        String path = exchange.getRequest().getPath().toString();

        /*if(!this.checker.hasPermission("POST", path)){
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }*/

        return webClient.post()
            .uri(path) // Use the dynamic path from the original request
            .body(exchange.getRequest().getBody(), String.class) // Forward the body
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> {
                // Handle connection errors gracefully
                return Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service"));
            });
    }

    @GetMapping("/**")
    public Mono<ResponseEntity<String>> forwardGetRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        
        return webClient.get()
            .uri(path)
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

    /*@PutMapping("/**")
    public Mono<ResponseEntity<String>> forwardPutRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();

        return webClient.put()
            .uri(path)
            .body(BodyInserters.fromProducer(exchange.getRequest().getBody(), String.class)) // Forward body
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

    @PatchMapping("/**")
    public Mono<ResponseEntity<String>> forwardPatchRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();

        return webClient.patch()
            .uri(path)
            .body(BodyInserters.fromProducer(exchange.getRequest().getBody(), String.class)) // Forward body
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

    @DeleteMapping("/**")
    public Mono<ResponseEntity<String>> forwardDeleteRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();

        return webClient.delete()
            .uri(path)
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }*/

}
