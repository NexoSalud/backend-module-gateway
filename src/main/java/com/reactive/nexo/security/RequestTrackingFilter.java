package com.reactive.nexo.security;

import com.reactive.nexo.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Filtro para guardar tracking de todas las llamadas POST, PATCH y PUT que pasan por el gateway.
 * Registra: empleado (x-employee-id), método y endpoint, y el status de respuesta.
 * No lee el cuerpo para evitar fugas de credenciales o datos sensibles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestTrackingFilter implements WebFilter {

    private final TrackingService trackingService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();

        // Solo registrar POST, PATCH, PUT
        boolean shouldTrack = method == HttpMethod.POST || method == HttpMethod.PATCH || method == HttpMethod.PUT;
        if (!shouldTrack) {
            return chain.filter(exchange);
        }

        final String endpoint = method.name() + " " + request.getPath().value();

        String employeeIdHeader = request.getHeaders().getFirst("x-employee-id");
        Long parsed = null;
        try {
            if (employeeIdHeader != null && !employeeIdHeader.isBlank()) {
                parsed = Long.parseLong(employeeIdHeader.trim());
            }
        } catch (NumberFormatException nfe) {
            log.warn("RequestTrackingFilter - employeeId inválido: {}", employeeIdHeader);
        }
        final Long empId = parsed;

        // Cachear y decorar el cuerpo para no romper el flujo downstream
        final AtomicReference<String> payloadRef = new AtomicReference<>(null);
        Mono<byte[]> cachedBody = DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String payloadStr = new String(bytes, StandardCharsets.UTF_8);
                    payloadRef.set(payloadStr);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .cache();

        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return cachedBody.flatMapMany(bytes -> {
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return Flux.just(buffer);
                });
            }
        };

        return cachedBody.then(
                chain.filter(exchange.mutate().request(decoratedRequest).build())
                        .onErrorResume(err -> {
                            String errMsg = err.getMessage() != null ? err.getMessage() : err.toString();
                            String payload = payloadRef.get();
                            return trackingService
                                    .logError(empId, "gateway", "Error en " + endpoint, errMsg)
                                    .then(Mono.error(err));
                        })
                        .then(Mono.defer(() -> {
                            ServerHttpResponse response = exchange.getResponse();
                            org.springframework.http.HttpStatusCode status = response.getStatusCode();
                            String responseInfo = "status=" + (status != null ? status.value() : 200);
                            String payload = payloadRef.get();
                            if (endpoint.contains("auth/login") ) {
                                payload = "{***}"; // Enmascarar payload de login para no guardar credenciales
                            }
                            return trackingService
                                    .logEndpointCall(empId, endpoint, payload, responseInfo)
                                    .then();
                        }))
        );
    }
}
