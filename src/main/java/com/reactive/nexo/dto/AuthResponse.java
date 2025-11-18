package com.reactive.nexo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Integer id;
    private String names;
    private String lastnames;
    private String identification_type;
    private String identification_number;
    private Integer rol_id;
    private String rol_nombre;
    private List<String> permisos;
}
