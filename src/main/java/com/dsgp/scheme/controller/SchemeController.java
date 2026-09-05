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
 * <p>Base path: {@code /schemes} (full path: {@code /api/v1/schemes}).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /schemes}      — create a new scheme (201 Created)</li>
 *   <li>{@code GET    /schemes}      — list all active schemes (200 OK)</li>
 *   <li>{@code GET    /schemes/{id}} — get scheme by ID (200 OK)</li>
 *   <li>{@code PUT    /schemes/{id}} — update scheme (200 OK)</li>
 *   <li>{@code DELETE /schemes/{id}} — soft-deactivate scheme (204 No Content)</li>
 * </ul>
 */
@RestController
@RequestMapping("/schemes")
@RequiredArgsConstructor
public class SchemeController {

    private final SchemeService schemeService;

    // ── POST /schemes ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<SchemeResponse> createScheme(
            @Valid @RequestBody SchemeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemeService.createScheme(request));
    }

    // ── GET /schemes ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<SchemeResponse>> getAllSchemes() {
        return ResponseEntity.ok(schemeService.getAllActiveSchemes());
    }

    // ── GET /schemes/{id} ────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<SchemeResponse> getSchemeById(@PathVariable Long id) {
        return ResponseEntity.ok(schemeService.getSchemeById(id));
    }

    // ── PUT /schemes/{id} ────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<SchemeResponse> updateScheme(
            @PathVariable Long id,
            @Valid @RequestBody SchemeRequest request) {

        return ResponseEntity.ok(schemeService.updateScheme(id, request));
    }

    // ── DELETE /schemes/{id} ─────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateScheme(@PathVariable Long id) {
        schemeService.deactivateScheme(id);
        return ResponseEntity.noContent().build();
    }
}