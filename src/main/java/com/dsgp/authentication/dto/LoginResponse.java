package com.dsgp.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private boolean success;
    private String message;
    private Integer beneficiaryId;
    private String name;
}