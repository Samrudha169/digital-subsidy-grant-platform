package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.AdminOfficerRequest;
import com.dsgp.authentication.dto.AdminOfficerResponse;

import java.util.List;

public interface AdminOfficerService {

    List<AdminOfficerResponse> getAllOfficers();

    AdminOfficerResponse getOfficerById(Long id);

    AdminOfficerResponse createOfficer(AdminOfficerRequest request);

    AdminOfficerResponse updateOfficer(Long id, AdminOfficerRequest request);

    AdminOfficerResponse deactivateOfficer(Long id);

    AdminOfficerResponse reactivateOfficer(Long id);
}