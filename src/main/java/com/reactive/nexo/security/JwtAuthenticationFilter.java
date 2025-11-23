package com.reactive.nexo.security;

import com.reactive.nexo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.List;
import com.reactive.nexo.security.PermissionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.net.InetSocketAddress;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final PermissionChecker permissionChecker; // Usamos la clase del ejemplo anterior

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.permissionChecker = new PermissionChecker();
    }

    @SuppressWarnings("unchecked")           
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/logout")) {
            return chain.filter(exchange);
        }
        
        String authorizationHeader = request.getHeaders().getFirst("Authorization");
        String employeeId = request.getHeaders().getFirst("x-employee-id");
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);        
        String ipAddress = (request.getRemoteAddress() != null) ? 
                          request.getRemoteAddress().getHostString() : "unknown";

        logger.info("Este es un mensaje de información: "+ authorizationHeader);
        logger.info("Este es un mensaje de información: "+ employeeId);
        logger.info("Este es un mensaje de información: "+ userAgent);
        logger.info("Este es un mensaje de información: "+ ipAddress);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7); // Quita "Bearer "

            try {
                Claims claims = jwtUtil.extractClaims(token);                
                
                if(!ipAddress.equals(claims.get("ip_address"))){      
                    logger.info("bad request: "+ ipAddress +" != "+ claims.get("ip_address"));
                    throw new Exception("Bad request");
                }
                if(!userAgent.equals(claims.get("user_agent"))){                    
                    logger.info("bad request: "+ userAgent +" != "+ claims.get("user_agent"));
                    throw new Exception("Bad request");
                }
                if(!employeeId.equals(claims.get("employee_id"))){                    
                    logger.info("bad request: "+ employeeId +" != "+ claims.get("employee_id"));
                    throw new Exception("Bad request");
                }
                
                List<Map<String, Object>> permissions = (List<Map<String, Object>>) claims.get("permissions");
                String requestMethod = request.getMethod().name();
                boolean hasPermission = permissionChecker.hasPermission(requestMethod, path, permissions);

                if (hasPermission) {
                    return chain.filter(exchange);
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); // 403 Forbidden
                    return exchange.getResponse().setComplete();
                }

            } catch (Exception e) {

                logger.info("Este es un mensaje de información: "+ e.getMessage());
                // Token inválido o expirado
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // 401 Unauthorized
                return exchange.getResponse().setComplete();
            }
        } else {
            // No se encontró encabezado Authorization
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // 401 Unauthorized
            return exchange.getResponse().setComplete();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
}
