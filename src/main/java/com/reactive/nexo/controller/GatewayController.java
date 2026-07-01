package com.reactive.nexo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import java.nio.charset.StandardCharsets;
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
    private final String error = "Gateway Error: Unknown service for path ";

    // URLs de servicios internos (usando nombres de contenedor Docker)
    //@Value("${service.users.url}")
    private String urlUsers = System.getenv().getOrDefault("USERS_SERVICE_URL", "http://localhost:8082");

    //@Value("${service.employees.url}")
    private String urlEmployees = System.getenv().getOrDefault("EMPLOYEES_SERVICE_URL", "http://localhost:8081");

    //@Value("${service.schedule.url}")
    private String urlSchedule = System.getenv().getOrDefault("SCHEDULE_SERVICE_URL", "http://localhost:8083");
    private String urlAppointments = System.getenv().getOrDefault("APPOINTMENTS_SERVICE_URL", "http://localhost:8084");
    private String urlHistory = System.getenv().getOrDefault("HISTORY_TEMPLATE_SERVICE_URL", "http://localhost:8085");
    private String urlConvenios = System.getenv().getOrDefault("CONVENIOS_SERVICE_URL", "http://localhost:8086");
    private String urlBilling = System.getenv().getOrDefault("BILLING_SERVICE_URL", "http://localhost:8087");
    private String urlClinicalRules = System.getenv().getOrDefault("CLINICAL_RULES_SERVICE_URL", "http://localhost:8089");
    private String urlSiau = System.getenv().getOrDefault("SIAU_SERVICE_URL", "http://localhost:8088");
    private String urlEbs = System.getenv().getOrDefault("EBS_CONTRACTS_SERVICE_URL", "http://localhost:8089");
    private String urlAuthSched = System.getenv().getOrDefault("AUTORIZACION_SERVICE_URL", "http://autorizacion-service:8090");
    private String urlUtils = System.getenv().getOrDefault("UTILS_SERVICE_URL", "http://localhost:8090");

    private final Map<String, WebClient>  webClients;

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClients = new HashMap<>();
        
        // Log de las URLs para depuración
        logger.info("URL Users: {}", urlUsers);
        logger.info("URL Employees: {}", urlEmployees);
        logger.info("URL Schedule: {}", urlSchedule);
        logger.info("URL Appointments: {}", urlAppointments);
        logger.info("URL Convenios: {}", urlConvenios);
        logger.info("URL Billing: {}", urlBilling);
        logger.info("URL Clinical Rules: {}", urlClinicalRules);
        logger.info("URL SIAU: {}", urlSiau);
        logger.info("URL EBS Contracts: {}", urlEbs);
        logger.info("URL Autorizacion y Agendamiento: {}", urlAuthSched);
        logger.info("URL Utils: {}", urlUtils);
        
        webClients.put("/api/v1/users", WebClient.create(urlUsers));
        webClients.put("/api/v1/employees", WebClient.create(urlEmployees));
        // Ruta usada por el frontend para agendas médicas
        webClients.put("/api/v1/schedule", WebClient.create(urlSchedule));
        webClients.put("/api/v1/medical-agenda", webClients.get("/api/v1/schedule"));
        webClients.put("/api/v1/appointments", WebClient.create(urlAppointments));
        webClients.put("/api/v1/rols", webClients.get("/api/v1/employees"));
        webClients.put("/api/v1/form-builder", WebClient.create(urlHistory));
        webClients.put("/graphql", webClients.get("/api/v1/form-builder"));
        webClients.put("/api/v1/convenios", WebClient.create(urlConvenios));
        webClients.put("/api/v1/billing", WebClient.create(urlBilling));
        webClients.put("/api/v1/clinical-rules", WebClient.create(urlClinicalRules));
        // Rutas de auth apuntan al servicio de employees (maneja login/JWT)
        webClients.put("/api/v1/auth", webClients.get("/api/v1/employees"));
        webClients.put("/api/v1/headquarters", webClients.get("/api/v1/employees"));
        webClients.put("/api/v1/service-types", webClients.get("/api/v1/schedule"));
        webClients.put("/api/v1/specialties", webClients.get("/api/v1/schedule"));
        webClients.put("/api/v1/tracking", webClients.get("/api/v1/employees"));
        // Rutas SIAU - PQRSDF
        webClients.put("/api/v1/siau", WebClient.create(urlSiau));
        // Rutas EBS - Gestión de Contratos
        webClients.put("/api/v1/ebs", WebClient.create(urlEbs));
        // Rutas Autorización y Agendamiento
        webClients.put("/api/v1/authorization-and-scheduling", WebClient.create(urlAuthSched));
        // Rutas Utils - Fuentes de Datos
        webClients.put("/api/v1/utils", WebClient.create(urlUtils));
        // GraphQL proxy routes for modules behind the gateway
        webClients.put("/utils/graphql", webClients.get("/api/v1/utils"));
     
    }
    private WebClient getWebClient(String path){
        if (path.startsWith("/history-template/graphql")) {
            return webClients.get("/api/v1/form-builder");
        }
        if (path.startsWith("/clinical-rules/graphql")) {
            return webClients.get("/api/v1/clinical-rules");
        }
        if (path.startsWith("/utils/graphql")) {
            return webClients.get("/api/v1/utils");
        }

        // Primero intentar match exacto con los primeros 3 segmentos (rutas /api/v1/xxx)
        String[] segments = path.split("/");
        String[] relevantSegments = Arrays.stream(segments)
                                          .filter(s -> !s.isEmpty())
                                          .toArray(String[]::new);

        int limit = Math.min(relevantSegments.length, 3);
        
        String result = Arrays.stream(relevantSegments)
                              .limit(limit)
                              .collect(Collectors.joining("/", "/", ""));
        
        logger.info("GatewayController.getWebClient - resolved key: {}", result);

        WebClient client = webClients.get(result);

        // Si no encontró con 3 segmentos, intentar con 1 segmento (ej: /graphql)
        if (client == null && relevantSegments.length >= 1) {
            String singleSegment = "/" + relevantSegments[0];
            logger.info("GatewayController.getWebClient - fallback to single segment: {}", singleSegment);
            client = webClients.get(singleSegment);
        }

        return client;
    }

    private String getTargetUri(String path, String queryString) {
        String targetPath = path;
        if (path.startsWith("/history-template/graphql")) {
            targetPath = "/graphql";
        } else if (path.startsWith("/clinical-rules/graphql")) {
            targetPath = "/graphql";
        } else if (path.startsWith("/utils/graphql")) {
            targetPath = "/graphql";
        }
        return buildCompleteUri(targetPath, queryString);
    }

    private String buildCompleteUri(String path, String queryString) {
        if (queryString != null && !queryString.isEmpty()) {
            return path + "?" + queryString;
        }
        return path;
    }

    /**
     * Forwards any POST request arriving at the root path "/" 
     * to the target service at http://localhost:8081/
     */
    @PostMapping("/**")
    public Mono<ResponseEntity<byte[]>> forwardPostRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        String completeUri = getTargetUri(path, exchange.getRequest().getURI().getQuery());
        
        WebClient webClient = this.getWebClient(path);
        if(webClient == null){
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getBytes()));
        }
        logger.info("GateWay -> Forwarding POST request to: " + completeUri);

        // Detect if this is a multipart upload (file upload)
        MediaType incomingContentType = exchange.getRequest().getHeaders().getContentType();
        boolean isMultipart = incomingContentType != null && 
            incomingContentType.toString().toLowerCase().contains("multipart/form-data");

        if (isMultipart) {
            // Forward raw bytes for multipart (preserve Content-Type with boundary)
            return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(rawBytes -> webClient.post()
                    .uri(completeUri)
                    .contentType(incomingContentType)
                    .bodyValue(rawBytes)
                    .exchangeToMono(clientResponse ->
                        clientResponse.bodyToMono(byte[].class)
                            .map(respBytes -> ResponseEntity.status(clientResponse.statusCode())
                                .headers(h -> h.addAll(clientResponse.headers().asHttpHeaders()))
                                .body(respBytes))
                            .defaultIfEmpty(ResponseEntity.status(clientResponse.statusCode()).build()))
                    .onErrorResume(e -> {
                        logger.error("Error forwarding multipart POST: ", e);
                        return Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service".getBytes()));
                    }));
        }

        // Standard JSON handling
        Mono<String> bodyMono = DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return new String(bytes, StandardCharsets.UTF_8);
            });

        return bodyMono.flatMap(body -> {
            logger.info("GateWay -> Body content: " + body);
            return webClient.post()
                .uri(completeUri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(clientResponse ->
                    clientResponse.bodyToMono(byte[].class)
                        .map(respBytes -> ResponseEntity.status(clientResponse.statusCode())
                            .headers(h -> h.addAll(clientResponse.headers().asHttpHeaders()))
                            .body(respBytes))
                        .defaultIfEmpty(ResponseEntity.status(clientResponse.statusCode()).build()))
                .onErrorResume(e -> {
                    logger.error("Error forwarding POST request: ", e);
                    return Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service".getBytes()));
                });
        });
    }

    @GetMapping("/**")
    public Mono<ResponseEntity<byte[]>> forwardGetRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        String completeUri = getTargetUri(path, exchange.getRequest().getURI().getQuery());
        
        WebClient webClient = this.getWebClient(path);

        if(webClient == null){
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getBytes()));
        }
        return webClient.get()
            .uri(completeUri)
            .headers(h -> h.addAll(exchange.getRequest().getHeaders()))
            .exchangeToMono(clientResponse ->
                clientResponse.bodyToMono(byte[].class)
                    .map(bytes -> ResponseEntity.status(clientResponse.statusCode())
                        .headers(h -> h.addAll(clientResponse.headers().asHttpHeaders()))
                        .body(bytes))
                    .defaultIfEmpty(ResponseEntity.status(clientResponse.statusCode()).build()))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service".getBytes())));
    }

    @PutMapping("/**")
    public Mono<ResponseEntity<byte[]>> forwardPutRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        String completeUri = getTargetUri(path, exchange.getRequest().getURI().getQuery());
        
        WebClient webClient = this.getWebClient(path);
        if(webClient == null){
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getBytes()));
        }
        Mono<String> bodyMono = DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return new String(bytes, StandardCharsets.UTF_8);
            });

        return bodyMono.flatMap(body -> {
            logger.info("GateWay -> Body content (PUT): " + body);
            return webClient.put()
                .uri(completeUri)
                .headers(h -> h.addAll(exchange.getRequest().getHeaders()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(clientResponse ->
                    clientResponse.bodyToMono(byte[].class)
                        .map(respBytes -> ResponseEntity.status(clientResponse.statusCode())
                            .headers(h -> h.addAll(clientResponse.headers().asHttpHeaders()))
                            .body(respBytes))
                        .defaultIfEmpty(ResponseEntity.status(clientResponse.statusCode()).build()))
                .onErrorResume(e -> {
                    logger.error("Error forwarding PUT request: ", e);
                    return Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service".getBytes()));
                });
        });
    }

    @PatchMapping("/**")
    public Mono<ResponseEntity<byte[]>> forwardPatchRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        String completeUri = getTargetUri(path, exchange.getRequest().getURI().getQuery());
        
        WebClient webClient = this.getWebClient(path);
        if(webClient == null){
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getBytes()));
        }
        Mono<String> bodyMono = DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return new String(bytes, StandardCharsets.UTF_8);
            });

        return bodyMono.flatMap(body -> {
            logger.info("GateWay -> Body content (PATCH): " + body);
            return webClient.patch()
                .uri(completeUri)
                .headers(h -> h.addAll(exchange.getRequest().getHeaders()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(clientResponse ->
                    clientResponse.bodyToMono(byte[].class)
                        .map(respBytes -> ResponseEntity.status(clientResponse.statusCode())
                            .headers(h -> h.addAll(clientResponse.headers().asHttpHeaders()))
                            .body(respBytes))
                        .defaultIfEmpty(ResponseEntity.status(clientResponse.statusCode()).build()))
                .onErrorResume(e -> {
                    logger.error("Error forwarding PATCH request: ", e);
                    return Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service".getBytes()));
                });
        });
    }

    @DeleteMapping("/**")
    public Mono<ResponseEntity<byte[]>> forwardDeleteRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        String completeUri = getTargetUri(path, exchange.getRequest().getURI().getQuery());
        
        WebClient webClient = this.getWebClient(path);
        if(webClient == null){
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getBytes()));
        }
        return webClient.delete()
            .uri(completeUri)
            .headers(h -> h.addAll(exchange.getRequest().getHeaders()))
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class)
                .map(bytes -> ResponseEntity.status(clientResponse.statusCode())
                    .headers(h -> h.addAll(clientResponse.headers().asHttpHeaders()))
                    .body(bytes))
                .defaultIfEmpty(ResponseEntity.status(clientResponse.statusCode()).build()))
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Gateway Error: Cannot reach backend service".getBytes())));
    }

}
