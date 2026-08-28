package com.dsgp.beneficiary.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BeneficiaryResponse {

    private Integer id;
    private String fullName;
    private String govId;
    private String contact;
    private String email;
    private Integer age;
    private String address;
    private String schemeName;
}