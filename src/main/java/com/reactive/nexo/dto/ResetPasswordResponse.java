package com.reactive.nexo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordResponse {
    private String identificationType;
    private String identificationNumber;
}
