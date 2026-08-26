package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.*;
import com.dsgp.beneficiary.entity.*;
import com.dsgp.beneficiary.exception.*;
import com.dsgp.beneficiary.repository.BeneficiaryDocumentRepository;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link BeneficiaryService}.
 *
 * <p>Handles beneficiary registration, CRUD operations, status management,
 * and document upload to the local filesystem (configured via
 * {@code app.storage.upload-dir}).
 *
 * <p>All write operations are transactional. Read operations use
 * {@code @Transactional(readOnly = true)} for performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryDocumentRepository documentRepository;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    // ── Registration ──────────────────────────────────────────────────────────

    @Override
    public BeneficiaryResponse registerBeneficiary(BeneficiaryRegistrationRequest request) {
        log.info("Registering new beneficiary, Aadhaar: {}", maskAadhaar(request.getAadhaarNumber()));

        if (beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new DuplicateAadhaarException(request.getAadhaarNumber());
        }
        if (beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException(request.getMobileNumber());
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .aadhaarNumber(request.getAadhaarNumber())
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .village(request.getVillage())
                .taluka(request.getTaluka())
                .district(request.getDistrict())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .annualIncome(request.getAnnualIncome())
                .landHolding(request.getLandHolding())
                .category(request.getCategory())
                .registrationStatus(RegistrationStatus.PENDING)
                .createdBy(request.getCreatedBy())
                .identityVerified(false)
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary registered with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    // ── Read Operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryByAadhaar(String aadhaarNumber) {
        Beneficiary beneficiary = beneficiaryRepository.findByAadhaarNumber(aadhaarNumber)
                .orElseThrow(() -> new BeneficiaryNotFoundException(
                        "Beneficiary not found with Aadhaar: " + maskAadhaar(aadhaarNumber)));
        return mapToResponse(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiarySummaryResponse> getBeneficiariesByStatus(
            RegistrationStatus status, Pageable pageable) {
        return beneficiaryRepository.findByRegistrationStatus(status, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiarySummaryResponse> getAllBeneficiaries(Pageable pageable) {
        return beneficiaryRepository.findAll(pageable).map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiarySummaryResponse> getBeneficiariesByDistrict(String district, Pageable pageable) {
        return beneficiaryRepository.findByDistrict(district, pageable)
                .map(this::mapToSummaryResponse);
    }

    // ── Update Operations ─────────────────────────────────────────────────────

    @Override
    public BeneficiaryResponse updateBeneficiary(Long id, BeneficiaryUpdateRequest request) {
        Beneficiary beneficiary = findById(id);
        log.info("Updating beneficiary ID: {}", id);

        if (request.getFirstName() != null)   beneficiary.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    beneficiary.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) beneficiary.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)      beneficiary.setGender(request.getGender());
        if (request.getEmail() != null)       beneficiary.setEmail(request.getEmail());
        if (request.getAddress() != null)     beneficiary.setAddress(request.getAddress());
        if (request.getVillage() != null)     beneficiary.setVillage(request.getVillage());
        if (request.getTaluka() != null)      beneficiary.setTaluka(request.getTaluka());
        if (request.getDistrict() != null)    beneficiary.setDistrict(request.getDistrict());
        if (request.getState() != null)       beneficiary.setState(request.getState());
        if (request.getPinCode() != null)     beneficiary.setPinCode(request.getPinCode());
        if (request.getAnnualIncome() != null) beneficiary.setAnnualIncome(request.getAnnualIncome());
        if (request.getLandHolding() != null)  beneficiary.setLandHolding(request.getLandHolding());
        if (request.getCategory() != null)    beneficiary.setCategory(request.getCategory());

        if (request.getMobileNumber() != null
                && !request.getMobileNumber().equals(beneficiary.getMobileNumber())) {
            if (beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
                throw new DuplicateMobileException(request.getMobileNumber());
            }
            beneficiary.setMobileNumber(request.getMobileNumber());
        }

        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    @Override
    public BeneficiaryResponse updateRegistrationStatus(Long id, RegistrationStatus status) {
        Beneficiary beneficiary = findById(id);
        log.info("Changing status of beneficiary ID {} to {}", id, status);
        beneficiary.setRegistrationStatus(status);
        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    // ── Document Operations ───────────────────────────────────────────────────

    @Override
    public DocumentResponse uploadDocument(Long beneficiaryId, String documentTypeStr,
                                           MultipartFile file, String uploadedBy) {
        Beneficiary beneficiary = findById(beneficiaryId);

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(documentTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentTypeException(
                    "Invalid document type: '" + documentTypeStr + "'. Allowed values: "
                            + Arrays.toString(DocumentType.values()));
        }

        // Reject duplicate document type for the same beneficiary
        if (documentRepository.existsByBeneficiaryIdAndDocumentType(beneficiaryId, documentType)) {
            throw new DuplicateDocumentTypeException(beneficiaryId, documentType.name());
        }

        validateFile(file);

        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path   targetDir      = Paths.get(uploadDir, "beneficiary", String.valueOf(beneficiaryId));
        Path   targetPath     = targetDir.resolve(storedFileName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store document for beneficiary {}: {}", beneficiaryId, e.getMessage());
            // Wrap with cause so GlobalExceptionHandler can return 500
            throw new DocumentUploadException("Failed to store document. Please try again.", e);
        }

        BeneficiaryDocument document = BeneficiaryDocument.builder()
                .beneficiary(beneficiary)
                .documentType(documentType)
                .fileName(storedFileName)
                .originalFileName(file.getOriginalFilename())
                .filePath(targetPath.toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(uploadedBy)
                .verified(false)
                .build();

        BeneficiaryDocument saved = documentRepository.save(document);
        log.info("Document {} uploaded for beneficiary ID {}", documentType, beneficiaryId);
        return mapToDocumentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(Long beneficiaryId) {
        findById(beneficiaryId); // validate existence
        return documentRepository.findByBeneficiaryId(beneficiaryId)
                .stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBeneficiary(Long id) {
        Beneficiary beneficiary = findById(id);
        log.info("Suspending beneficiary ID: {}", id);
        beneficiary.setRegistrationStatus(RegistrationStatus.SUSPENDED);
        beneficiaryRepository.save(beneficiary);
    }

    @Override
    public BeneficiaryResponse verifyIdentity(Long id) {
        Beneficiary beneficiary = findById(id);
        log.info("Marking identity as verified for beneficiary ID: {}", id);
        beneficiary.setIdentityVerified(true);
        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Beneficiary findById(Long id) {
        return beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentUploadException("Uploaded file is empty or missing");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.equals("application/pdf")
                &&  !contentType.equals("image/jpeg")
                &&  !contentType.equals("image/png"))) {
            throw new DocumentUploadException(
                    "Unsupported file type: " + contentType + ". Allowed types: PDF, JPEG, PNG");
        }
    }

    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) return "****";
        return "XXXX-XXXX-" + aadhaar.substring(aadhaar.length() - 4);
    }

    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .firstName(b.getFirstName())
                .lastName(b.getLastName())
                .dateOfBirth(b.getDateOfBirth())
                .gender(b.getGender())
                .aadhaarNumber(b.getAadhaarNumber())
                .mobileNumber(b.getMobileNumber())
                .email(b.getEmail())
                .address(b.getAddress())
                .village(b.getVillage())
                .taluka(b.getTaluka())
                .district(b.getDistrict())
                .state(b.getState())
                .pinCode(b.getPinCode())
                .annualIncome(b.getAnnualIncome())
                .landHolding(b.getLandHolding())
                .category(b.getCategory())
                .registrationStatus(b.getRegistrationStatus())
                .registrationDate(b.getRegistrationDate())
                .createdBy(b.getCreatedBy())
                .updatedAt(b.getUpdatedAt())
                .identityVerified(b.isIdentityVerified())
                .build();
    }

    private BeneficiarySummaryResponse mapToSummaryResponse(Beneficiary b) {
        return BeneficiarySummaryResponse.builder()
                .id(b.getId())
                .firstName(b.getFirstName())
                .lastName(b.getLastName())
                .aadhaarNumber(b.getAadhaarNumber())
                .mobileNumber(b.getMobileNumber())
                .district(b.getDistrict())
                .registrationStatus(b.getRegistrationStatus())
                .identityVerified(b.isIdentityVerified())
                .build();
    }

    private DocumentResponse mapToDocumentResponse(BeneficiaryDocument d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .beneficiaryId(d.getBeneficiary().getId())
                .documentType(d.getDocumentType())
                .originalFileName(d.getOriginalFileName())
                .fileName(d.getFileName())
                .fileSize(d.getFileSize())
                .mimeType(d.getMimeType())
                .uploadedAt(d.getUploadedAt())
                .uploadedBy(d.getUploadedBy())
                .verified(d.isVerified())
                .build();
    }
}
