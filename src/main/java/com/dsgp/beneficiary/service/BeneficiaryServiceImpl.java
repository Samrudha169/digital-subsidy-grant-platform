package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.BeneficiaryDocument;
import com.dsgp.beneficiary.entity.DocumentType;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.exception.DocumentUploadException;
import com.dsgp.beneficiary.exception.DuplicateAadhaarException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.exception.InvalidDocumentTypeException;
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
import java.nio.file.StandardCopyOption;
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

        // Legacy Government ID
        if (request.getGovId() != null
                && beneficiaryRepository.existsByGovId(request.getGovId())) {

            throw new DuplicateAadhaarException(request.getGovId());
        }

        // Legacy contact
        if (request.getContact() != null
                && beneficiaryRepository.existsByContact(request.getContact())) {

            throw new DuplicateMobileException(request.getContact());
        }

        // Aadhaar
        if (request.getAadhaarNumber() != null
                && beneficiaryRepository.existsByAadhaarNumber(
                request.getAadhaarNumber())) {

            throw new DuplicateAadhaarException(
                    request.getAadhaarNumber());
        }

        // Mobile
        if (request.getMobileNumber() != null
                && beneficiaryRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new DuplicateMobileException(
                    request.getMobileNumber());
        }

        Beneficiary beneficiary = Beneficiary.builder()

                // ------------------------------------------------
                // Legacy fields
                // ------------------------------------------------
                .fullName(request.getFullName())
                .govId(request.getGovId())
                .contact(request.getContact())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(request.getPassword())
                )
                .age(request.getAge())
                .address(request.getAddress())
                .schemeName(request.getSchemeName())

                // ------------------------------------------------
                // Identity fields
                // ------------------------------------------------
                .aadhaarNumber(request.getAadhaarNumber())
                .mobileNumber(request.getMobileNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())

                // ------------------------------------------------
                // Structured address
                // ------------------------------------------------
                .village(request.getVillage())
                .taluka(request.getTaluka())
                .district(request.getDistrict())
                .state(request.getState())
                .pinCode(request.getPinCode())

                // ------------------------------------------------
                // Eligibility fields
                // ------------------------------------------------
                .annualIncome(request.getAnnualIncome())
                .landHolding(request.getLandHolding())
                .category(request.getCategory())
                .occupation(request.getOccupation())

                .registrationStatus(RegistrationStatus.PENDING)
                .identityVerified(false)

                .build();

        Beneficiary saved =
                beneficiaryRepository.save(beneficiary);

        log.info(
                "Beneficiary registered successfully with ID: {}",
                saved.getId()
        );

        return mapToResponse(saved);
    }

    // ============================================================
    // GET BENEFICIARY BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Integer id) {

        Beneficiary beneficiary =
                beneficiaryRepository.findById(id)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(id)
                        );

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // GET BENEFICIARY BY GOVERNMENT ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryByGovId(String govId) {

        Beneficiary beneficiary =
                beneficiaryRepository.findByGovId(govId)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(
                                        "Beneficiary not found with Government ID: "
                                                + govId
                                )
                        );

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // GET ALL BENEFICIARIES
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

        Beneficiary beneficiary =
                beneficiaryRepository.findById(id)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(id)
                        );

        // Government ID duplicate check
        if (request.getGovId() != null
                && !request.getGovId().equals(
                beneficiary.getGovId())
                && beneficiaryRepository.existsByGovId(
                request.getGovId())) {

            throw new DuplicateAadhaarException(
                    request.getGovId()
            );
        }

        // Contact duplicate check
        if (request.getContact() != null
                && !request.getContact().equals(
                beneficiary.getContact())
                && beneficiaryRepository.existsByContact(
                request.getContact())) {

            throw new DuplicateMobileException(
                    request.getContact()
            );
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

        Beneficiary beneficiary =
                beneficiaryRepository.findById(id)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(id)
                        );

        // --------------------------------------------------------
        // Name
        // --------------------------------------------------------

        if (request.getFirstName() != null) {
            beneficiary.setFirstName(
                    request.getFirstName()
            );
        }

        if (request.getLastName() != null) {
            beneficiary.setLastName(
                    request.getLastName()
            );
        }

        // --------------------------------------------------------
        // Personal details
        // --------------------------------------------------------

        if (request.getDateOfBirth() != null) {
            beneficiary.setDateOfBirth(
                    request.getDateOfBirth()
            );
        }

        if (request.getGender() != null) {
            beneficiary.setGender(
                    request.getGender()
            );
        }

        // --------------------------------------------------------
        // Mobile
        // --------------------------------------------------------

        if (request.getMobileNumber() != null) {

            if (!request.getMobileNumber().equals(
                    beneficiary.getMobileNumber())
                    && beneficiaryRepository.existsByMobileNumber(
                    request.getMobileNumber())) {

                throw new DuplicateMobileException(
                        request.getMobileNumber()
                );
            }

            beneficiary.setMobileNumber(
                    request.getMobileNumber()
            );

            // Keep legacy contact synchronized
            beneficiary.setContact(
                    request.getMobileNumber()
            );
        }

        // --------------------------------------------------------
        // Email
        // --------------------------------------------------------

        if (request.getEmail() != null) {
            beneficiary.setEmail(
                    request.getEmail()
            );
        }

        // --------------------------------------------------------
        // Address
        // --------------------------------------------------------

        if (request.getAddress() != null) {
            beneficiary.setAddress(
                    request.getAddress()
            );
        }

        if (request.getVillage() != null) {
            beneficiary.setVillage(
                    request.getVillage()
            );
        }

        if (request.getTaluka() != null) {
            beneficiary.setTaluka(
                    request.getTaluka()
            );
        }

        if (request.getDistrict() != null) {
            beneficiary.setDistrict(
                    request.getDistrict()
            );
        }

        if (request.getState() != null) {
            beneficiary.setState(
                    request.getState()
            );
        }

        if (request.getPinCode() != null) {
            beneficiary.setPinCode(
                    request.getPinCode()
            );
        }

        // --------------------------------------------------------
        // Eligibility fields
        // --------------------------------------------------------

        if (request.getAnnualIncome() != null) {
            beneficiary.setAnnualIncome(
                    request.getAnnualIncome()
            );
        }

        if (request.getLandHolding() != null) {
            beneficiary.setLandHolding(
                    request.getLandHolding()
            );
        }

        if (request.getCategory() != null) {
            beneficiary.setCategory(
                    request.getCategory()
            );
        }

        /*
         * Government ID / Aadhaar is intentionally not updated
         * through the patch request.
         */

        beneficiaryRepository.save(beneficiary);

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // DELETE BENEFICIARY
    // ============================================================

    @Override
    public void deleteBeneficiary(Integer id) {

        Beneficiary beneficiary =
                beneficiaryRepository.findById(id)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(id)
                        );

        beneficiaryRepository.delete(beneficiary);
    }

    // ============================================================
    // DOCUMENT UPLOAD
    // ============================================================

    @Override
    public DocumentResponse uploadDocument(
            Integer beneficiaryId,
            MultipartFile file,
            DocumentType documentType,
            String uploadedBy) throws IOException {

        Beneficiary beneficiary =
                beneficiaryRepository.findById(beneficiaryId)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(
                                        beneficiaryId
                                )
                        );

        if (documentType == null) {
            throw new InvalidDocumentTypeException(
                    "Document type is required."
            );
        }

        validateFile(file);

        /*
         * IMPORTANT:
         *
         * If this beneficiary already has this document type,
         * delete the old database record and old physical file.
         *
         * This means the latest uploaded document replaces
         * the previous document instead of appearing twice.
         */

        documentRepository
                .findByBeneficiaryIdAndDocumentType(
                        beneficiaryId.longValue(),
                        documentType
                )
                .ifPresent(oldDocument -> {

                    deletePhysicalFile(oldDocument);

                    documentRepository.delete(oldDocument);

                    log.info(
                            "Replaced previous {} document for beneficiary {}",
                            documentType,
                            beneficiaryId
                    );
                });

        String originalFileName =
                file.getOriginalFilename();

        String storedFileName =
                UUID.randomUUID()
                        + "_"
                        + (originalFileName == null
                        ? "document"
                        : originalFileName);

        Path targetDir =
                Paths.get(
                        uploadDir,
                        "beneficiary",
                        String.valueOf(beneficiaryId)
                );

        Path targetPath =
                targetDir.resolve(storedFileName);

        try {

            Files.createDirectories(targetDir);

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            log.error(
                    "Failed to store document for beneficiary {}: {}",
                    beneficiaryId,
                    e.getMessage()
            );

            throw new DocumentUploadException(
                    "Failed to store document. Please try again.",
                    e
            );
        }

        BeneficiaryDocument document =
                BeneficiaryDocument.builder()
                        .beneficiary(beneficiary)
                        .documentType(documentType)
                        .fileName(storedFileName)
                        .originalFileName(originalFileName)
                        .filePath(targetPath.toString())
                        .fileSize(file.getSize())
                        .mimeType(file.getContentType())
                        .uploadedBy(uploadedBy)
                        .verified(false)
                        .build();

        BeneficiaryDocument saved =
                documentRepository.save(document);

        log.info(
                "Document {} uploaded for beneficiary {}",
                documentType,
                beneficiaryId
        );

        return mapToDocumentResponse(saved);
    }

    // ============================================================
    // GET DOCUMENTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(
            Integer beneficiaryId) {

        // Validate beneficiary exists
        beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(
                        () -> new BeneficiaryNotFoundException(
                                beneficiaryId
                        )
                );

        return documentRepository
                .findByBeneficiaryId(beneficiaryId.longValue())
                .stream()
                .map(this::mapToDocumentResponse)
                .toList();
    }

    // ============================================================
    // GET SINGLE DOCUMENT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(
            Integer beneficiaryId,
            Long documentId) {

        BeneficiaryDocument document =
                documentRepository.findById(documentId)
                        .orElseThrow(
                                () -> new DocumentUploadException(
                                        "Document not found."
                                )
                        );

        if (document.getBeneficiary() == null
                || !beneficiaryId.equals(
                document.getBeneficiary().getId())) {

            throw new DocumentUploadException(
                    "Document does not belong to this beneficiary."
            );
        }

        return mapToDocumentResponse(document);
    }

    // ============================================================
    // DOWNLOAD DOCUMENT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocument(
            Integer beneficiaryId,
            Long documentId) throws IOException {

        BeneficiaryDocument document =
                documentRepository.findById(documentId)
                        .orElseThrow(
                                () -> new DocumentUploadException(
                                        "Document not found."
                                )
                        );

        if (document.getBeneficiary() == null
                || !beneficiaryId.equals(
                document.getBeneficiary().getId())) {

            throw new DocumentUploadException(
                    "Document does not belong to this beneficiary."
            );
        }

        Path path =
                Paths.get(document.getFilePath());

        if (!Files.exists(path)) {
            throw new DocumentUploadException(
                    "Document file is missing from storage."
            );
        }

        return Files.readAllBytes(path);
    }

    // ============================================================
    // VERIFY IDENTITY
    // ============================================================

    @Override
    public BeneficiaryResponse verifyIdentity(
            Integer beneficiaryId,
            String verifiedBy) {

        Beneficiary beneficiary =
                beneficiaryRepository.findById(beneficiaryId)
                        .orElseThrow(
                                () -> new BeneficiaryNotFoundException(
                                        beneficiaryId
                                )
                        );

        beneficiary.setIdentityVerified(true);

        log.info(
                "Identity verified for beneficiary {} by {}",
                beneficiaryId,
                verifiedBy
        );

        beneficiaryRepository.save(beneficiary);

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new DocumentUploadException(
                    "Uploaded file is empty or missing."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || (!contentType.equals(
                "application/pdf")
                && !contentType.equals(
                "image/jpeg")
                && !contentType.equals(
                "image/png")
                && !contentType.equals(
                "image/webp"))) {

            throw new DocumentUploadException(
                    "Unsupported file type: "
                            + contentType
                            + ". Allowed types: PDF, JPEG, PNG, WEBP."
            );
        }
    }

    private void deletePhysicalFile(
            BeneficiaryDocument document) {

        try {

            if (document.getFilePath() != null) {

                Path path =
                        Paths.get(
                                document.getFilePath()
                        );

                Files.deleteIfExists(path);
            }

        } catch (IOException e) {

            log.warn(
                    "Could not delete old document file: {}",
                    document.getFilePath()
            );
        }
    }

    private String maskAadhaar(
            String aadhaar) {

        if (aadhaar == null
                || aadhaar.length() < 4) {

            return "****";
        }

        return "XXXX-XXXX-"
                + aadhaar.substring(
                aadhaar.length() - 4
        );
    }

    private BeneficiaryResponse mapToResponse(
            Beneficiary b) {

        return BeneficiaryResponse.builder()
                .id(b.getId())

                // Legacy fields
                .fullName(b.getFullName())
                .govId(b.getGovId())
                .contact(b.getContact())

                // Extended fields
                .firstName(b.getFirstName())
                .lastName(b.getLastName())
                .dateOfBirth(b.getDateOfBirth())
                .gender(b.getGender())
                .aadhaarNumber(b.getAadhaarNumber())
                .mobileNumber(b.getMobileNumber())

                .email(b.getEmail())
                .age(b.getAge())
                .address(b.getAddress())
                .schemeName(b.getSchemeName())

                .village(b.getVillage())
                .taluka(b.getTaluka())
                .district(b.getDistrict())
                .state(b.getState())
                .pinCode(b.getPinCode())

                .annualIncome(b.getAnnualIncome())
                .landHolding(b.getLandHolding())
                .category(b.getCategory())

                .registrationStatus(
                        b.getRegistrationStatus()
                )
                .identityVerified(
                        b.isIdentityVerified()
                )

                .build();
    }

    private DocumentResponse mapToDocumentResponse(
            BeneficiaryDocument d) {

        return DocumentResponse.builder()
                .id(d.getId())
                .beneficiaryId(
                        d.getBeneficiary().getId().longValue()
                )
                .documentType(
                        d.getDocumentType()
                )
                .originalFileName(
                        d.getOriginalFileName()
                )
                .fileName(
                        d.getFileName()
                )
                .fileSize(
                        d.getFileSize()
                )
                .mimeType(
                        d.getMimeType()
                )
                .uploadedAt(
                        d.getUploadedAt()
                )
                .uploadedBy(
                        d.getUploadedBy()
                )
                .verified(
                        d.isVerified()
                )
                .build();
    }
}