package com.reactive.nexo.service;

import com.reactive.nexo.model.Tracking;
import com.reactive.nexo.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final TrackingRepository trackingRepository;

    /**
     * Registra una nueva acción en el sistema de tracking
     */
    public Mono<Tracking> logAction(Long employeeId, String action, String data, String result) {
        log.debug("Registrando acción: employeeId={}, action={}", employeeId, action);
        
        Tracking tracking = new Tracking(employeeId, action, data, result);
        return trackingRepository.save(tracking)
                .doOnSuccess(saved -> log.debug("Acción registrada con ID: {}", saved.getId()))
                .doOnError(error -> log.error("Error al registrar acción", error));
    }

    /**
     * Registra una acción con solo la acción y datos (empleado y resultado opcionales)
     */
    public Mono<Tracking> logAction(String action, String data) {
        return logAction(null, action, data, null);
    }

    /**
     * Registra una llamada a endpoint
     */
    public Mono<Tracking> logEndpointCall(Long employeeId, String endpoint, String payload, String response) {
        String action = "endpoint called: " + endpoint;
        return logAction(employeeId, action, payload, response);
    }

    /**
     * Registra una acción de autenticación
     */
    public Mono<Tracking> logAuthAction(Long employeeId, String authAction, String details) {
        String action = "auth: " + authAction;
        return logAction(employeeId, action, details, null);
    }

    /**
     * Registra un error del sistema
     */
    public Mono<Tracking> logError(Long employeeId, String errorType, String errorDetails, String stackTrace) {
        String action = "error: " + errorType;
        return logAction(employeeId, action, errorDetails, stackTrace);
    }
}