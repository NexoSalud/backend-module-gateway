package com.reactive.nexo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("tracking")
@Schema(description = "Entidad que registra todas las acciones ejecutadas en el sistema")
public class Tracking {

    @Id
    @Schema(description = "ID único del registro de tracking", example = "1")
    private Long id;
    
    @Schema(description = "Fecha y hora de creación del registro")
    private LocalDateTime createdAt;
    
    @Schema(description = "ID del empleado que ejecutó la acción", example = "1")
    private Long employeeId;
    
    @Schema(description = "Descripción de la acción ejecutada", example = "endpoint called", defaultValue = "endpoint called")
    private String action;
    
    @Schema(description = "Datos del payload de la petición", example = "{\"param1\": \"value1\"}")
    private String data;
    
    @Schema(description = "Resultado de la respuesta", example = "{\"status\": \"success\"}")
    private String result;

    // Constructor para crear tracking sin ID (para inserts)
    public Tracking(Long employeeId, String action, String data, String result) {
        this.employeeId = employeeId;
        this.action = action != null ? action : "endpoint called";
        this.data = data;
        this.result = result;
        this.createdAt = LocalDateTime.now();
    }

    // Método auxiliar para configurar created_at personalizado
    public Tracking withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}