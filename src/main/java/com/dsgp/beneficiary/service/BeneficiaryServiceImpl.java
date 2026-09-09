package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.BeneficiaryDocument;
import com.dsgp.beneficiary.entity.DocumentType;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.exception.DuplicateAadhaarException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.repository.BeneficiaryDocumentRepository;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryDocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    // ============================================================
    // REGISTER BENEFICIARY
    // ============================================================

    @Override
    public BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request) {

        // Check legacy Government ID
        if (beneficiaryRepository.existsByGovId(request.getGovId())) {
            throw new DuplicateAadhaarException(request.getGovId());
        }

        // Check legacy contact number
        if (beneficiaryRepository.existsByContact(request.getContact())) {
            throw new DuplicateMobileException(request.getContact());
        }

        // Check Aadhaar if supplied
        if (request.getAadhaarNumber() != null
                && beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new DuplicateAadhaarException(request.getAadhaarNumber());
        }

        // Check mobile number if supplied
        if (request.getMobileNumber() != null
                && beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException(request.getMobileNumber());
        }

        Beneficiary beneficiary = Beneficiary.builder()
                // Legacy fields
                .fullName(request.getFullName())
                .govId(request.getGovId())
                .contact(request.getContact())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .age(request.getAge())
                .address(request.getAddress())
                .schemeName(request.getSchemeName())

                // Extended identity fields
                .aadhaarNumber(request.getAadhaarNumber())
                .mobileNumber(request.getMobileNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())

                // Structured address
                .village(request.getVillage())
                .taluka(request.getTaluka())
                .district(request.getDistrict())
                .state(request.getState())
                .pinCode(request.getPinCode())

                // Eligibility fields
                .annualIncome(request.getAnnualIncome())
                .landHolding(request.getLandHolding())
                .category(request.getCategory())

                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        return mapToResponse(saved);
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Integer id) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // GET BY GOVERNMENT ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryByGovId(String govId) {

        Beneficiary beneficiary = beneficiaryRepository.findByGovId(govId)
                .orElseThrow(() ->
                        new BeneficiaryNotFoundException(
                                "Beneficiary not found with Government ID: " + govId
                        ));

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiaries() {

        return beneficiaryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ============================================================
    // LEGACY UPDATE
    // ============================================================

    @Override
    public BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        if (!request.getGovId().equals(beneficiary.getGovId())
                && beneficiaryRepository.existsByGovId(request.getGovId())) {

            throw new DuplicateAadhaarException(request.getGovId());
        }

        if (!request.getContact().equals(beneficiary.getContact())
                && beneficiaryRepository.existsByContact(request.getContact())) {

            throw new DuplicateMobileException(request.getContact());
        }

        beneficiary.setFullName(request.getFullName());
        beneficiary.setGovId(request.getGovId());
        beneficiary.setContact(request.getContact());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setAge(request.getAge());
        beneficiary.setAddress(request.getAddress());
        beneficiary.setSchemeName(request.getSchemeName());

        /*
         * Password is intentionally not changed here.
         * Password changes should be handled separately.
         */

        beneficiaryRepository.save(beneficiary);

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // MILESTONE 2 PATCH UPDATE
    // ============================================================

    @Override
    public BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryUpdateRequest request) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        // --------------------------------------------------------
        // Name
        // --------------------------------------------------------

        if (request.getFirstName() != null) {
            beneficiary.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            beneficiary.setLastName(request.getLastName());
        }

        // --------------------------------------------------------
        // Personal details
        // --------------------------------------------------------

        if (request.getDateOfBirth() != null) {
            beneficiary.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            beneficiary.setGender(request.getGender());
        }

        // --------------------------------------------------------
        // Mobile
        // --------------------------------------------------------

        if (request.getMobileNumber() != null) {

            if (!request.getMobileNumber().equals(beneficiary.getMobileNumber())
                    && beneficiaryRepository.existsByMobileNumber(
                    request.getMobileNumber())) {

                throw new DuplicateMobileException(
                        request.getMobileNumber());
            }

            beneficiary.setMobileNumber(request.getMobileNumber());

            // Keep legacy contact synchronized
            beneficiary.setContact(request.getMobileNumber());
        }

        // --------------------------------------------------------
        // Email
        // --------------------------------------------------------

        if (request.getEmail() != null) {
            beneficiary.setEmail(request.getEmail());
        }

        // --------------------------------------------------------
        // Address
        // --------------------------------------------------------

        if (request.getAddress() != null) {
            beneficiary.setAddress(request.getAddress());
        }

        if (request.getVillage() != null) {
            beneficiary.setVillage(request.getVillage());
        }

        if (request.getTaluka() != null) {
            beneficiary.setTaluka(request.getTaluka());
        }

        if (request.getDistrict() != null) {
            beneficiary.setDistrict(request.getDistrict());
        }

        if (request.getState() != null) {
            beneficiary.setState(request.getState());
        }

        if (request.getPinCode() != null) {
            beneficiary.setPinCode(request.getPinCode());
        }

        // --------------------------------------------------------
        // Eligibility fields
        // --------------------------------------------------------

        if (request.getAnnualIncome() != null) {
            beneficiary.setAnnualIncome(request.getAnnualIncome());
        }

        if (request.getLandHolding() != null) {
            beneficiary.setLandHolding(request.getLandHolding());
        }

        if (request.getCategory() != null) {
            beneficiary.setCategory(request.getCategory());
        }

        /*
         * Government ID / Aadhaar is intentionally NOT updated.
         */

        beneficiaryRepository.save(beneficiary);

        /*
         * Return the managed entity instead of relying on save()
         * returning a value. This also makes the method robust when
         * mocked in unit tests.
         */
        return mapToResponse(beneficiary);
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteBeneficiary(Integer id) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        beneficiaryRepository.delete(beneficiary);
    }

    // ============================================================
    // ENTITY → RESPONSE
    // ============================================================

    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {

        return BeneficiaryResponse.builder()
                // Original fields
                .id(beneficiary.getId())
                .fullName(beneficiary.getFullName())
                .govId(beneficiary.getGovId())
                .contact(beneficiary.getContact())
                .email(beneficiary.getEmail())
                .age(beneficiary.getAge())
                .address(beneficiary.getAddress())
                .schemeName(beneficiary.getSchemeName())

                // Extended identity fields
                .aadhaarNumber(beneficiary.getAadhaarNumber())
                .mobileNumber(beneficiary.getMobileNumber())
                .firstName(beneficiary.getFirstName())
                .lastName(beneficiary.getLastName())
                .dateOfBirth(beneficiary.getDateOfBirth())
                .gender(beneficiary.getGender())

                // Structured address
                .village(beneficiary.getVillage())
                .taluka(beneficiary.getTaluka())
                .district(beneficiary.getDistrict())
                .state(beneficiary.getState())
                .pinCode(beneficiary.getPinCode())

                // Eligibility fields
                .annualIncome(beneficiary.getAnnualIncome())
                .landHolding(beneficiary.getLandHolding())
                .category(beneficiary.getCategory())

                // Lifecycle fields
                .registrationStatus(beneficiary.getRegistrationStatus())
                .identityVerified(beneficiary.isIdentityVerified())

                .build();
    }

    // ============================================================
    // DOCUMENT UPLOAD
    // ============================================================

    @Override
    public DocumentResponse uploadDocument(Integer beneficiaryId,
                                           MultipartFile file,
                                           DocumentType documentType,
                                           String uploadedBy) throws IOException {

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new BeneficiaryNotFoundException(beneficiaryId));

        // Build storage directory: uploads/beneficiary/{id}/
        Path storageDir = Paths.get(uploadDir, "beneficiary", String.valueOf(beneficiaryId));
        Files.createDirectories(storageDir);

        // Unique filename to prevent collisions
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path targetPath = storageDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetPath);

        BeneficiaryDocument document = BeneficiaryDocument.builder()
                .beneficiary(beneficiary)
                .documentType(documentType)
                .fileName(storedFilename)
                .originalFileName(originalFilename)
                .filePath(targetPath.toAbsolutePath().toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(uploadedBy)
                .build();

        BeneficiaryDocument saved = documentRepository.save(document);

        log.info("Document uploaded: beneficiaryId={}, type={}, file={}",
                beneficiaryId, documentType, storedFilename);

        return mapDocumentToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(Integer beneficiaryId) {

        // Verify beneficiary exists
        if (!beneficiaryRepository.existsById(beneficiaryId)) {
            throw new BeneficiaryNotFoundException(beneficiaryId);
        }

        return documentRepository.findByBeneficiaryId(beneficiaryId.longValue())
                .stream()
                .map(this::mapDocumentToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Integer beneficiaryId, Long documentId) {

        BeneficiaryDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BeneficiaryNotFoundException(
                        "Document not found with ID: " + documentId));

        if (!doc.getBeneficiary().getId().equals(beneficiaryId)) {
            throw new BeneficiaryNotFoundException(
                    "Document " + documentId + " does not belong to beneficiary " + beneficiaryId);
        }

        return mapDocumentToResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocument(Integer beneficiaryId, Long documentId) throws IOException {

        BeneficiaryDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BeneficiaryNotFoundException(
                        "Document not found with ID: " + documentId));

        if (!doc.getBeneficiary().getId().equals(beneficiaryId)) {
            throw new BeneficiaryNotFoundException(
                    "Document " + documentId + " does not belong to beneficiary " + beneficiaryId);
        }

        Path filePath = Paths.get(doc.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("File not found on server: " + doc.getFilePath());
        }

        return Files.readAllBytes(filePath);
    }

    // ============================================================
    // IDENTITY VERIFICATION
    // ============================================================

    @Override
    public BeneficiaryResponse verifyIdentity(Integer beneficiaryId, String verifiedBy) {

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new BeneficiaryNotFoundException(beneficiaryId));

        if (beneficiary.isIdentityVerified()) {
            log.info("Identity already verified for beneficiaryId={}", beneficiaryId);
        } else {
            beneficiary.setIdentityVerified(true);
            beneficiaryRepository.save(beneficiary);
            log.info("Identity verified for beneficiaryId={} by officer={}",
                    beneficiaryId, verifiedBy);
        }

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // DOCUMENT ENTITY → DTO
    // ============================================================

    private DocumentResponse mapDocumentToResponse(BeneficiaryDocument doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .beneficiaryId(doc.getBeneficiary().getId().longValue())
                .documentType(doc.getDocumentType())
                .originalFileName(doc.getOriginalFileName())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .uploadedAt(doc.getUploadedAt())
                .uploadedBy(doc.getUploadedBy())
                .verified(doc.isVerified())
                .build();
    }
}