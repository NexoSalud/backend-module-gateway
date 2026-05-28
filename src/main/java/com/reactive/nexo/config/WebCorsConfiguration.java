package com.reactive.nexo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Lee orígenes permitidos desde variable de entorno CORS_ALLOWED_ORIGINS.
        // Formato: lista separada por comas, ej: "https://app.nexosalud.com,http://localhost:3000"
        // Si no está definida, permite todos los orígenes (*).
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");

        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            List<String> origins = Arrays.asList(allowedOriginsEnv.split(","));
            corsConfig.setAllowedOriginPatterns(origins);
        } else {
            // Sin restricción de origen — válido para desarrollo y cuando el
            // frontend está en un dominio/IP desconocido en producción.
            corsConfig.setAllowedOriginPatterns(List.of("*"));
        }

        corsConfig.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "x-employee-id",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        corsConfig.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // allowCredentials=true es incompatible con allowedOriginPatterns("*")
        // Solo se activa si hay orígenes explícitos configurados.
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            corsConfig.setAllowCredentials(true);
        } else {
            corsConfig.setAllowCredentials(false);
        }

        corsConfig.setExposedHeaders(List.of(
            "Authorization",
            "x-employee-id",
            "Location"
        ));

        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
