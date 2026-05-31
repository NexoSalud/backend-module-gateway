package com.reactive.nexo.security;

import com.reactive.nexo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final PermissionChecker permissionChecker;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.permissionChecker = new PermissionChecker();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        logger.info("JwtFilter - path: {}", path);

        // Rutas públicas — no requieren JWT
        if (path.startsWith("/api/v1/auth")
                || path.startsWith("/graphql")
                || path.startsWith("/api/v1/graphql")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        String authorizationHeader = request.getHeaders().getFirst("Authorization");
        String employeeId = request.getHeaders().getFirst("x-employee-id");
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        String ipAddress = (request.getRemoteAddress() != null)
                ? request.getRemoteAddress().getHostString() : "unknown";

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            try {
                Claims claims = jwtUtil.extractClaims(token);

                // Validar IP — solo si no hay proxy (X-Forwarded-For ausente)
                String claimedIp = (String) claims.get("ip_address");
                if (claimedIp != null && !claimedIp.equals(ipAddress)) {
                    // Verificar si viene de proxy — en ese caso no validar IP
                    String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
                    if (forwardedFor == null) {
                        logger.warn("JwtFilter - IP mismatch: {} != {}", ipAddress, claimedIp);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                }

                // Validar UserAgent
                String claimedAgent = (String) claims.get("user_agent");
                if (claimedAgent != null && userAgent != null && !claimedAgent.equals(userAgent)) {
                    logger.warn("JwtFilter - UserAgent mismatch");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                // Validar employeeId
                String claimedEmployeeId = claims.get("employee_id") != null
                        ? claims.get("employee_id").toString() : null;
                if (claimedEmployeeId != null && employeeId != null
                        && !claimedEmployeeId.equals(employeeId)) {
                    logger.warn("JwtFilter - EmployeeId mismatch: {} != {}", employeeId, claimedEmployeeId);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                List<Map<String, Object>> permissions = (List<Map<String, Object>>) claims.get("permissions");
                String requestMethod = request.getMethod().name();
                boolean hasPermission = permissionChecker.hasPermission(requestMethod, path, permissions);

                if (hasPermission) {
                    return chain.filter(exchange);
                } else {
                    logger.warn("JwtFilter - No permission for {} {}", requestMethod, path);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

            } catch (Exception e) {
                logger.error("JwtFilter - Token error: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } else {
            logger.warn("JwtFilter - No Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
