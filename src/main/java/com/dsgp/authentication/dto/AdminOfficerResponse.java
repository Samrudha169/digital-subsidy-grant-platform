package com.dsgp.authentication.dto;

import com.dsgp.authentication.entity.OfficerRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOfficerResponse {

    private Long id;

    private String username;

    private String fullName;

    private String email;

    private OfficerRole role;

    private String district;

    private boolean active;
}