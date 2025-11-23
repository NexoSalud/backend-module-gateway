package com.reactive.nexo.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

//@RestController
// Maps all paths dynamically
public class GatewayController2 {

    private final WebClient webClient;
    private final String TARGET_URL = "http://localhost:8081";

    public GatewayController2(WebClient.Builder webClientBuilder) {
        // Base URL is set to simplify the .uri() call
        this.webClient = webClientBuilder.baseUrl(TARGET_URL).build();
    }

    /**
     * Generic handler to forward any GET, POST, PUT, PATCH, or DELETE request
     * to the target service.
     */
    /*@RequestMapping(value = "/any/**", method = {RequestMethod.GET, RequestMethod.POST, 
                                            RequestMethod.PUT, RequestMethod.DELETE, 
                                            RequestMethod.PATCH})
    public Mono<ResponseEntity<String>> forwardRequest(ServerWebExchange exchange) {
        
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().toString();

        // Start building the WebClient request based on the original method and path
        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(path);

        // For requests that can have a body (POST, PUT, PATCH), we need to forward it.
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            requestSpec.body(BodyInserters.fromProducer(exchange.getRequest().getBody(), String.class));
        }
        // GET and DELETE methods do not have a body, so we just proceed.

        // Execute the request and map the response to a ResponseEntity
        return requestSpec.exchangeToMono(clientResponse -> 
                clientResponse.toEntity(String.class))
            .onErrorResume(e -> {
                // Handle connection errors gracefully
                return Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service"));
            });
    }*/
}
