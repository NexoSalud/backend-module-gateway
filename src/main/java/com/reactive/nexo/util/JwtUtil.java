package com.reactive.nexo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidation1234567890}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")  // Default 1 hour in milliseconds
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate a JWT token with user claims (id, username, roles, permisos)
     */
    public String generateToken(Integer userId, String username, String rolNombre, List<String> permisos) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("username", username);
        if (rolNombre != null) {
            claims.put("rol", rolNombre);
        }
        if (permisos != null && !permisos.isEmpty()) {
            claims.put("permisos", permisos);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract claims from a token
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Failed to extract claims from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate a token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user_id from token
     */
    public Integer extractUserId(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return null;
        Object userIdObj = claims.get("user_id");
        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).intValue();
        }
        return null;
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return null;
        return (String) claims.get("username");
    }

    /**
     * Extract roles/permissions from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermisos(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return null;
        return (List<String>) claims.get("permisos");
    }

    /**
     * Extract rol_nombre from token
     */
    public String extractRol(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return null;
        return (String) claims.get("rol");
    }
}
