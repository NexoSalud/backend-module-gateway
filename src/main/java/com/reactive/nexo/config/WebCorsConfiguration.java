package com.reactive.nexo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro CORS global con la máxima prioridad.
 *
 * Inyecta los headers Access-Control-* en TODAS las respuestas que
 * salen del gateway, incluyendo las que vienen de microservicios
 * forwarded por GatewayController. Esto evita tener que configurar
 * CORS en cada microservicio individualmente.
 */
@Configuration
public class WebCorsConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            // Origen permitido: el que viene en el request, o * si no hay
            String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
            if (origin != null && !origin.isBlank()) {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            } else {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }

            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    "GET, POST, PUT, DELETE, PATCH, OPTIONS");

            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    "Authorization, Content-Type, Accept, x-employee-id, " +
                    "X-Requested-With, Access-Control-Request-Method, " +
                    "Access-Control-Request-Headers");

            headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    "Authorization, x-employee-id, Location");

            headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");

            // Responder inmediatamente a preflight OPTIONS sin pasar al controller
            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }
}
