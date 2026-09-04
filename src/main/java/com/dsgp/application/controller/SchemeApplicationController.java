package com.dsgp.application.controller;

import com.dsgp.application.dto.VerificationRequest;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.application.service.SchemeApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/applications")
public class SchemeApplicationController {

    private final SchemeApplicationService schemeApplicationService;

    public SchemeApplicationController(
            SchemeApplicationService schemeApplicationService) {
        this.schemeApplicationService = schemeApplicationService;
    }

    // Submit a new scheme application
    @PostMapping
    public ResponseEntity<SchemeApplication> submitApplication(
            @RequestBody SchemeApplication application) {

        return ResponseEntity.ok(
                schemeApplicationService.submitApplication(application)
        );
    }

    // Get an application by ID
    @GetMapping("/{applicationId}")
    public ResponseEntity<SchemeApplication> getApplication(
            @PathVariable Long applicationId) {

        return ResponseEntity.ok(
                schemeApplicationService.getApplicationById(applicationId)
        );
    }

    // Verify an application at the current verification level
    @PostMapping("/{applicationId}/verify")
    public ResponseEntity<SchemeApplication> verifyApplication(
            @PathVariable Long applicationId,
            @RequestBody VerificationRequest request) {

        return ResponseEntity.ok(
                schemeApplicationService.verifyApplication(
                        applicationId,
                        request
                )
        );
    }

    // Request re-verification of an application
    @PostMapping("/{applicationId}/reverify")
    public ResponseEntity<SchemeApplication> requestReVerification(
            @PathVariable Long applicationId,
            @RequestParam(required = false) String remarks) {

        return ResponseEntity.ok(
                schemeApplicationService.requestReVerification(
                        applicationId,
                        remarks
                )
        );
    }
}