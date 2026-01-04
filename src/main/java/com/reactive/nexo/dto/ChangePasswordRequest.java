package com.reactive.nexo.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String employee_email;
    private String new_password;
}