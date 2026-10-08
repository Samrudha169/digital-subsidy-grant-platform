package com.dsgp.authentication.controller;

import com.dsgp.authentication.dto.AdminOfficerRequest;
import com.dsgp.authentication.dto.AdminOfficerResponse;
import com.dsgp.authentication.service.AdminOfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/officers")
@RequiredArgsConstructor
public class AdminOfficerController {

    private final AdminOfficerService adminOfficerService;

    @GetMapping
    public List<AdminOfficerResponse> getAllOfficers() {
        return adminOfficerService.getAllOfficers();
    }

    @GetMapping("/{id}")
    public AdminOfficerResponse getOfficerById(
            @PathVariable Long id
    ) {
        return adminOfficerService.getOfficerById(id);
    }

    @PostMapping
    public AdminOfficerResponse createOfficer(
            @RequestBody AdminOfficerRequest request
    ) {
        return adminOfficerService.createOfficer(request);
    }

    @PutMapping("/{id}")
    public AdminOfficerResponse updateOfficer(
            @PathVariable Long id,
            @RequestBody AdminOfficerRequest request
    ) {
        return adminOfficerService.updateOfficer(id, request);
    }

    @DeleteMapping("/{id}")
    public AdminOfficerResponse deactivateOfficer(
            @PathVariable Long id
    ) {
        return adminOfficerService.deactivateOfficer(id);
    }

    @PutMapping("/{id}/reactivate")
    public AdminOfficerResponse reactivateOfficer(
            @PathVariable Long id
    ) {
        return adminOfficerService.reactivateOfficer(id);
    }
}