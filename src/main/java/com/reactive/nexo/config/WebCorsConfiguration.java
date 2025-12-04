package com.reactive.nexo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class WebCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Permitir orígenes específicos incluyendo tu ambiente local
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "http://local.nexosalud:*",
            "https://local.nexosalud:*",
            "http://127.0.0.1:*",
            "https://127.0.0.1:*"
        ));
        
        // Permitir todos los headers necesarios
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "x-employee-id",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Permitir todos los métodos HTTP
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Permitir envío de credenciales (cookies, headers de autorización)
        corsConfig.setAllowCredentials(true);
        
        // Exponer headers personalizados en la respuesta
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "x-employee-id",
            "Location"
        ));
        
        // Configurar tiempo de cache para preflight requests
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}