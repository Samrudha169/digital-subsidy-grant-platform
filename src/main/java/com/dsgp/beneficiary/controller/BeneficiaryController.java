package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    // Register a new beneficiary
    @PostMapping
    public ResponseEntity<BeneficiaryResponse> registerBeneficiary(
            @Valid @RequestBody BeneficiaryRegistrationRequest request) {

        BeneficiaryResponse response =
                beneficiaryService.registerBeneficiary(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get beneficiary by ID
    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                beneficiaryService.getBeneficiaryById(id)
        );
    }

    // Get beneficiary by Government ID
    @GetMapping("/gov-id/{govId}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryByGovId(
            @PathVariable String govId) {

        return ResponseEntity.ok(
                beneficiaryService.getBeneficiaryByGovId(govId)
        );
    }

    // Get all beneficiaries
    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getAllBeneficiaries() {

        return ResponseEntity.ok(
                beneficiaryService.getAllBeneficiaries()
        );
    }

    // Update beneficiary
    @PutMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> updateBeneficiary(
            @PathVariable Integer id,
            @Valid @RequestBody BeneficiaryRegistrationRequest request) {

        return ResponseEntity.ok(
                beneficiaryService.updateBeneficiary(id, request)
        );
    }

    // Delete beneficiary
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficiary(
            @PathVariable Integer id) {

        beneficiaryService.deleteBeneficiary(id);

        return ResponseEntity.noContent().build();
    }
}