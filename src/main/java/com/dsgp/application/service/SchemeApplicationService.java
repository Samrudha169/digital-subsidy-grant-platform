package com.dsgp.application.service;

import com.dsgp.application.dto.VerificationRequest;
import com.dsgp.application.entity.SchemeApplication;

public interface SchemeApplicationService {

    SchemeApplication submitApplication(SchemeApplication application);

    SchemeApplication getApplicationById(Long applicationId);

    SchemeApplication verifyApplication(
            Long applicationId,
            VerificationRequest request
    );
}