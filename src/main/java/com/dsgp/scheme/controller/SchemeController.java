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

    // Create a new scheme
    @PostMapping
    public ResponseEntity<Scheme> createScheme(@RequestBody Scheme scheme) {
        return ResponseEntity.ok(schemeService.createScheme(scheme));
    }

    // Get all schemes
    @GetMapping
    public ResponseEntity<List<Scheme>> getAllSchemes() {
        return ResponseEntity.ok(schemeService.getAllSchemes());
    }

    // Get scheme by ID
    @GetMapping("/{id}")
    public ResponseEntity<Scheme> getSchemeById(@PathVariable Long id) {
        return ResponseEntity.ok(schemeService.getSchemeById(id));
    }

    // Get only active schemes
    @GetMapping("/active")
    public ResponseEntity<List<Scheme>> getActiveSchemes() {
        return ResponseEntity.ok(schemeService.getActiveSchemes());
    }

    // Update a scheme
    @PutMapping("/{id}")
    public ResponseEntity<Scheme> updateScheme(
            @PathVariable Long id,
            @RequestBody Scheme scheme) {

        return ResponseEntity.ok(
                schemeService.updateScheme(id, scheme)
        );
    }

    // Deactivate a scheme
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheme(@PathVariable Long id) {
        schemeService.deleteScheme(id);
        return ResponseEntity.noContent().build();
    }
}