package com.reactive.nexo.controller;

import com.reactive.nexo.model.Tracking;
import com.reactive.nexo.repository.TrackingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tracking", description = "API para consultar registros de tracking de acciones ejecutadas")
public class TrackingController {

    private final TrackingRepository trackingRepository;

    @Operation(
        summary = "Consultar tracking por atributo y valor",
        description = "Permite consultar registros de tracking filtrando por cualquier atributo de la tabla " +
                     "con diferentes tipos de relación (eq=igual, lt=menor que, gt=mayor que). " +
                     "Atributos soportados: id, created_at, employee_id, action, data, result"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
        @ApiResponse(responseCode = "400", description = "Atributo no válido o formato de fecha incorrecto"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/by/{attribute}/{value}")
    public Mono<ResponseEntity<Flux<Tracking>>> getTrackingBy(
            @Parameter(description = "Atributo de la tabla (id, created_at, employee_id, action, data, result)", required = true)
            @PathVariable String attribute,
            
            @Parameter(description = "Valor a buscar", required = true)
            @PathVariable String value,
            
            @Parameter(description = "Tipo de relación (eq, lt, gt)", example = "eq")
            @RequestParam(defaultValue = "eq") String relation) {

        log.info("Consultando tracking por atributo: {}, valor: {}, relación: {}", attribute, value, relation);

        try {
            Flux<Tracking> result = getTrackingFlux(attribute, value, relation);
            return Mono.just(ResponseEntity.ok(result));
        } catch (IllegalArgumentException e) {
            log.error("Error en parámetros: {}", e.getMessage());
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al consultar tracking", e);
            return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor"));
        }
    }

    @Operation(
        summary = "Obtener últimos registros de tracking",
        description = "Devuelve los últimos registros de tracking ordenados por fecha de creación"
    )
    @GetMapping("/latest")
    public Mono<ResponseEntity<Flux<Tracking>>> getLatestTracking(
            @Parameter(description = "Número de registros a obtener", example = "50")
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Obteniendo los últimos {} registros de tracking", limit);
        
        Flux<Tracking> result = trackingRepository.findLatestTracking(limit);
        return Mono.just(ResponseEntity.ok(result));
    }

    @Operation(
        summary = "Obtener todos los registros de tracking",
        description = "Devuelve todos los registros de tracking ordenados por fecha de creación descendente"
    )
    @GetMapping
    public Mono<ResponseEntity<Flux<Tracking>>> getAllTracking() {
        log.info("Obteniendo todos los registros de tracking");
        
        Flux<Tracking> result = trackingRepository.findAll()
                .sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
        return Mono.just(ResponseEntity.ok(result));
    }

    private Flux<Tracking> getTrackingFlux(String attribute, String value, String relation) {
        // Normalizar la relación
        relation = relation.toLowerCase();

        // Validar relación
        if (!relation.equals("eq") && !relation.equals("lt") && !relation.equals("gt")) {
            throw new IllegalArgumentException("Relación no válida. Use: eq, lt, gt");
        }

        // Procesar según el atributo
        switch (attribute.toLowerCase()) {
            case "id":
                return getByIdAttribute(value, relation);
            case "created_at":
                return getByCreatedAtAttribute(value, relation);
            case "employee_id":
                return getByEmployeeIdAttribute(value, relation);
            case "action":
                return getByActionAttribute(value, relation);
            case "data":
                return getByDataAttribute(value, relation);
            case "result":
                return getByResultAttribute(value, relation);
            default:
                throw new IllegalArgumentException("Atributo no válido: " + attribute + 
                    ". Atributos soportados: id, created_at, employee_id, action, data, result");
        }
    }

    private Flux<Tracking> getByIdAttribute(String value, String relation) {
        try {
            Long longValue = Long.parseLong(value);
            switch (relation) {
                case "eq": return trackingRepository.findByIdEquals(longValue);
                case "lt": return trackingRepository.findByIdLessThan(longValue);
                case "gt": return trackingRepository.findByIdGreaterThan(longValue);
                default: throw new IllegalArgumentException("Relación no válida");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor para 'id' debe ser un número válido");
        }
    }

    private Flux<Tracking> getByEmployeeIdAttribute(String value, String relation) {
        try {
            Long longValue = Long.parseLong(value);
            switch (relation) {
                case "eq": return trackingRepository.findByEmployeeIdEquals(longValue);
                case "lt": return trackingRepository.findByEmployeeIdLessThan(longValue);
                case "gt": return trackingRepository.findByEmployeeIdGreaterThan(longValue);
                default: throw new IllegalArgumentException("Relación no válida");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor para 'employee_id' debe ser un número válido");
        }
    }

    private Flux<Tracking> getByCreatedAtAttribute(String value, String relation) {
        try {
            // Intentar parsear diferentes formatos de fecha
            LocalDateTime dateValue;
            try {
                // Formato ISO estándar: 2023-12-01T10:30:00
                dateValue = LocalDateTime.parse(value);
            } catch (DateTimeParseException e1) {
                try {
                    // Formato fecha sin tiempo: 2023-12-01
                    dateValue = LocalDateTime.parse(value + "T00:00:00");
                } catch (DateTimeParseException e2) {
                    throw new IllegalArgumentException("Formato de fecha no válido. Use: 2023-12-01T10:30:00 o 2023-12-01");
                }
            }
            
            switch (relation) {
                case "eq": return trackingRepository.findByCreatedAtEquals(dateValue);
                case "lt": return trackingRepository.findByCreatedAtLessThan(dateValue);
                case "gt": return trackingRepository.findByCreatedAtGreaterThan(dateValue);
                default: throw new IllegalArgumentException("Relación no válida");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha no válido para 'created_at'. Use: 2023-12-01T10:30:00 o 2023-12-01");
        }
    }

    private Flux<Tracking> getByActionAttribute(String value, String relation) {
        switch (relation) {
            case "eq": return trackingRepository.findByActionEquals(value);
            case "lt": return trackingRepository.findByActionLessThan(value);
            case "gt": return trackingRepository.findByActionGreaterThan(value);
            default: throw new IllegalArgumentException("Relación no válida");
        }
    }

    private Flux<Tracking> getByDataAttribute(String value, String relation) {
        switch (relation) {
            case "eq": return trackingRepository.findByDataEquals(value);
            case "lt": return trackingRepository.findByDataLessThan(value);
            case "gt": return trackingRepository.findByDataGreaterThan(value);
            default: throw new IllegalArgumentException("Relación no válida");
        }
    }

    private Flux<Tracking> getByResultAttribute(String value, String relation) {
        switch (relation) {
            case "eq": return trackingRepository.findByResultEquals(value);
            case "lt": return trackingRepository.findByResultLessThan(value);
            case "gt": return trackingRepository.findByResultGreaterThan(value);
            default: throw new IllegalArgumentException("Relación no válida");
        }
    }
}