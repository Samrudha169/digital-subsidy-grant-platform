package com.dsgp.authentication.dto;

import com.dsgp.authentication.entity.OfficerRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOfficerRequest {

    private String username;

    private String password;

    private String fullName;

    private String email;

    private OfficerRole role;

    private String district;
}