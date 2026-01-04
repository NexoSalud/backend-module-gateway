package com.reactive.nexo.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String identificationType;
    private String identificationNumber;
}
