package com.reactive.nexo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import com.reactive.nexo.security.PermissionChecker;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class GatewayController {


    // Inyectar las URLs desde application.properties
    //@Value("${service.users.url}")
    private String urlUsers = "http://localhost:8082";

    //@Value("${service.employees.url}")
    private String urlEmployees = "http://localhost:8081";

    private final Map<String, WebClient>  webClients;

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClients = new HashMap<>(); 
        webClients.put("/api/v1/users", webClientBuilder.baseUrl(urlUsers).build());
        webClients.put("/api/v1/employees", webClientBuilder.baseUrl(urlEmployees).build());
     
    }
    private WebClient getWebClient(String path){
        String[] segments = path.split("/");
        String[] relevantSegments = Arrays.stream(segments)
                                          .filter(s -> !s.isEmpty())
                                          .toArray(String[]::new);

        int limit = Math.min(relevantSegments.length, 3);
        
        String result = Arrays.stream(relevantSegments)
                              .limit(limit)
                              .collect(Collectors.joining("/", "/", ""));
        
        logger.info("Este es un mensaje de información: "+ result);
        return webClients.get(result);
    }

    /**
     * Forwards any POST request arriving at the root path "/" 
     * to the target service at http://localhost:8081/
     */
    @PostMapping("/**") // Maps all POST paths dynamically
    public Mono<ResponseEntity<String>> forwardPostRequests(ServerWebExchange exchange) {
   
        String path = exchange.getRequest().getPath().toString();

        return this.getWebClient(path).post()
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
        
        return this.getWebClient(path).get()
            .uri(path)
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

    @PutMapping("/**")
    public Mono<ResponseEntity<String>> forwardPutRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();

        return this.getWebClient(path).put()
            .uri(path)
            .body(BodyInserters.fromProducer(exchange.getRequest().getBody(), String.class)) // Forward body
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

    @PatchMapping("/**")
    public Mono<ResponseEntity<String>> forwardPatchRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();

        return this.getWebClient(path).patch()
            .uri(path)
            .body(BodyInserters.fromProducer(exchange.getRequest().getBody(), String.class)) // Forward body
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

    @DeleteMapping("/**")
    public Mono<ResponseEntity<String>> forwardDeleteRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();

        return this.getWebClient(path).delete()
            .uri(path)
            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service")));
    }

}
