package com.reactive.nexo.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String employee_id;
    private String new_password;
}