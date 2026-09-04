package com.dsgp.scheme.controller;

import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.scheme.service.SchemeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schemes")
public class SchemeController {

    private final SchemeService schemeService;

    public SchemeController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    // Get all schemes
    @GetMapping
    public ResponseEntity<List<Scheme>> getAllSchemes() {
        return ResponseEntity.ok(
                schemeService.getAllSchemes()
        );
    }

    // Get only active schemes
    @GetMapping("/active")
    public ResponseEntity<List<Scheme>> getActiveSchemes() {
        return ResponseEntity.ok(
                schemeService.getActiveSchemes()
        );
    }

    // Get a particular scheme by ID
    @GetMapping("/{schemeId}")
    public ResponseEntity<Scheme> getSchemeById(
            @PathVariable Long schemeId) {

        return ResponseEntity.ok(
                schemeService.getSchemeById(schemeId)
        );
    }
}