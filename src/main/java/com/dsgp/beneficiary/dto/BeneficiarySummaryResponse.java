package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.RegistrationStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Lightweight summary DTO for paginated beneficiary list responses.
 *
 * <p>Used by {@code GET /beneficiaries} and {@code GET /beneficiaries/district/{district}}
 * to return essential fields without the full beneficiary payload.
 */
@Data
@Builder
public class BeneficiarySummaryResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String aadhaarNumber;
    private String mobileNumber;
    private String district;
    private RegistrationStatus registrationStatus;
    private boolean identityVerified;
}
