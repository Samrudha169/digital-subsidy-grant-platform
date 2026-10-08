package com.dsgp.scheme.controller;

import com.dsgp.scheme.dto.SchemeRequest;
import com.dsgp.scheme.dto.SchemeResponse;
import com.dsgp.scheme.service.SchemeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for government scheme management.
 *
 * <p>Base path: {@code /schemes}
 * (full path: {@code /api/v1/schemes}).</p>
 */
@RestController
@RequestMapping("/schemes")
@RequiredArgsConstructor
public class SchemeController {

    private final SchemeService schemeService;

    // ── POST /schemes ─────────────────────────────────────────────────────────

    /**
     * Create a new scheme.
     */
    @PostMapping
    public ResponseEntity<SchemeResponse> createScheme(
            @Valid @RequestBody SchemeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemeService.createScheme(request));
    }

    // ── GET /schemes ──────────────────────────────────────────────────────────

    /**
     * Get all active schemes.
     *
     * <p>This endpoint remains unchanged so the existing
     * beneficiary-facing scheme list continues to show
     * only active schemes.</p>
     */
    @GetMapping
    public ResponseEntity<List<SchemeResponse>> getAllSchemes() {

        return ResponseEntity.ok(
                schemeService.getAllActiveSchemes()
        );
    }

    // ── GET /schemes/admin/all ────────────────────────────────────────────────

    /**
     * Get all schemes for Admin management.
     *
     * <p>Includes both active and inactive schemes.</p>
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<SchemeResponse>> getAllSchemesForAdmin() {

        return ResponseEntity.ok(
                schemeService.getAllSchemes()
        );
    }

    // ── GET /schemes/{id} ─────────────────────────────────────────────────────

    /**
     * Get scheme by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchemeResponse> getSchemeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                schemeService.getSchemeById(id)
        );
    }

    // ── PUT /schemes/{id} ─────────────────────────────────────────────────────

    /**
     * Update an existing scheme.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SchemeResponse> updateScheme(
            @PathVariable Long id,
            @Valid @RequestBody SchemeRequest request) {

        return ResponseEntity.ok(
                schemeService.updateScheme(id, request)
        );
    }

    // ── DELETE /schemes/{id} ──────────────────────────────────────────────────

    /**
     * Soft-deactivate a scheme.
     *
     * <p>The scheme is not physically deleted from the database.</p>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateScheme(
            @PathVariable Long id) {

        schemeService.deactivateScheme(id);

        return ResponseEntity.noContent().build();
    }

    // ── PUT /schemes/{id}/reactivate ──────────────────────────────────────────

    /**
     * Reactivate a previously deactivated scheme.
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<SchemeResponse> reactivateScheme(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                schemeService.reactivateScheme(id)
        );
    }
}