package com.dsgp.beneficiary.service;

import com.dsgp.authentication.service.EmailOtpService;
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
import com.dsgp.beneficiary.exception.DuplicateEmailException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.exception.InvalidDocumentTypeException;
import com.dsgp.beneficiary.repository.BeneficiaryDocumentRepository;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final EmailOtpService emailOtpService;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    // ============================================================
    // REGISTER BENEFICIARY
    // ============================================================

    @Override
    public BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request) {

        // Email — must be unique; checked before any other duplicate to give the
        // clearest error message and to prevent the downstream NonUniqueResultException
        // that would occur when sendOtp() tries to look up the saved beneficiary.
        if (request.getEmail() != null
                && beneficiaryRepository.existsByEmail(request.getEmail())) {

            throw new DuplicateEmailException(request.getEmail());
        }

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
                .emailVerified(false)

                .build();

        Beneficiary saved =
                beneficiaryRepository.save(beneficiary);

        log.info(
                "Beneficiary registered successfully with ID: {}",
                saved.getId()
        );

        // Send OTP only after the beneficiary transaction commits.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                emailOtpService.sendOtp(saved.getEmail());
                            } catch (MailException e) {
                                // TEMPORARY: log full cause chain to expose SMTP response code
                                log.error(
                                        "Failed to send OTP email for beneficiary ID {} — " +
                                                "registration succeeded but email not delivered: {}",
                                        saved.getId(),
                                        e.getMessage()
                                );
                                Throwable cause = e.getCause();
                                int depth = 0;
                                while (cause != null && depth < 5) {
                                    log.error("[SMTP-DIAG] cause[{}]: {} — {}",
                                            depth, cause.getClass().getName(), cause.getMessage());
                                    cause = cause.getCause();
                                    depth++;
                                }
                            } catch (IllegalArgumentException e) {
                                // Should not normally occur after a successful registration commit,
                                // but guard against transient race conditions (e.g. very fast delete).
                                log.error(
                                        "Could not issue OTP for beneficiary ID {} after commit: {}",
                                        saved.getId(),
                                        e.getMessage()
                                );
                            }
                        }
                    }
            );
        }

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
            beneficiary.setCategory(request.getCategory());
        }

        if (request.getOccupation() != null) {
            beneficiary.setOccupation(request.getOccupation());
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

        /*
         * Existing/legacy document upload.
         *
         * Stage-specific documents must use the new method below.
         */
        return uploadDocument(
                beneficiaryId,
                null,
                null,
                file,
                documentType,
                uploadedBy
        );
    }


// ============================================================
// STAGE-SPECIFIC DOCUMENT UPLOAD
// ============================================================

    @Override
    public DocumentResponse uploadDocument(
            Integer beneficiaryId,
            Long applicationId,
            Integer stageNumber,
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

        /*
         * Validate stage-specific document information.
         *
         * Stage 2 and Stage 3 documents MUST have:
         *
         * applicationId
         * stageNumber
         */
        validateStageDocumentMetadata(
                applicationId,
                stageNumber,
                documentType
        );

        validateFile(file);

        /*
         * For normal beneficiary documents:
         *
         * beneficiary + documentType
         *
         * For stage documents:
         *
         * beneficiary + application + stage + documentType
         *
         * This prevents a Stage 2 invoice from replacing
         * a Stage 3 document or a document from another application.
         */
        if (applicationId != null && stageNumber != null) {

            documentRepository
                    .findByBeneficiaryIdAndApplicationIdAndStageNumberAndDocumentType(
                            beneficiaryId.longValue(),
                            applicationId,
                            stageNumber,
                            documentType
                    )
                    .ifPresent(oldDocument -> {

                        deletePhysicalFile(oldDocument);

                        documentRepository.delete(oldDocument);

                        log.info(
                                "Replaced previous {} document for beneficiary {}, application {}, stage {}",
                                documentType,
                                beneficiaryId,
                                applicationId,
                                stageNumber
                        );
                    });

        } else {

            /*
             * Existing behaviour for normal beneficiary documents.
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
        }

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

        /*
         * Create database record.
         */
        BeneficiaryDocument document =
                BeneficiaryDocument.builder()
                        .beneficiary(beneficiary)
                        .applicationId(applicationId)
                        .stageNumber(stageNumber)
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
                "Document {} uploaded for beneficiary {}, application {}, stage {}",
                documentType,
                beneficiaryId,
                applicationId,
                stageNumber
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
// GET DOCUMENTS BY APPLICATION AND STAGE
// ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByApplicationAndStage(
            Integer beneficiaryId,
            Long applicationId,
            Integer stageNumber) {

        // Validate beneficiary exists
        beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(
                        () -> new BeneficiaryNotFoundException(
                                beneficiaryId
                        )
                );

        if (applicationId == null) {
            throw new IllegalArgumentException(
                    "Application ID is required."
            );
        }

        if (stageNumber == null
                || (stageNumber != 2 && stageNumber != 3)) {

            throw new IllegalArgumentException(
                    "Stage number must be 2 or 3."
            );
        }

        return documentRepository
                .findByBeneficiaryIdAndApplicationIdAndStageNumber(
                        beneficiaryId.longValue(),
                        applicationId,
                        stageNumber
                )
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

    private void validateStageDocumentMetadata(
            Long applicationId,
            Integer stageNumber,
            DocumentType documentType) {

        boolean stage2Document =
                documentType == DocumentType.STAGE_2_INVOICE
                        || documentType == DocumentType.STAGE_2_PAYMENT_PROOF
                        || documentType == DocumentType.STAGE_2_ACTIVITY_PHOTO;

        boolean stage3Document =
                documentType == DocumentType.FINAL_COMPLETION_REPORT
                        || documentType == DocumentType.UTILIZATION_STATEMENT
                        || documentType == DocumentType.FINAL_PROJECT_PHOTO
                        || documentType == DocumentType.FINAL_PAYMENT_PROOF;

        /*
         * Normal beneficiary documents do not need
         * applicationId or stageNumber.
         */
        if (!stage2Document && !stage3Document) {
            return;
        }

        /*
         * Stage documents MUST belong to an application.
         */
        if (applicationId == null) {
            throw new IllegalArgumentException(
                    "Application ID is required for stage documents."
            );
        }

        /*
         * Stage number is mandatory.
         */
        if (stageNumber == null) {
            throw new IllegalArgumentException(
                    "Stage number is required for stage documents."
            );
        }

        /*
         * Stage 2 documents can only be uploaded to Stage 2.
         */
        if (stage2Document && stageNumber != 2) {
            throw new IllegalArgumentException(
                    "Stage 2 documents can only be uploaded for Stage 2."
            );
        }

        /*
         * Stage 3 documents can only be uploaded to Stage 3.
         */
        if (stage3Document && stageNumber != 3) {
            throw new IllegalArgumentException(
                    "Stage 3 documents can only be uploaded for Stage 3."
            );
        }
    }


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

    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .fullName(b.getFullName())
                .govId(b.getGovId())
                .contact(b.getContact())
                .email(b.getEmail())
                .age(b.getAge())
                .address(b.getAddress())
                .schemeName(b.getSchemeName())
                .occupation(b.getOccupation())
                .aadhaarNumber(b.getAadhaarNumber())
                .mobileNumber(b.getMobileNumber())
                .firstName(b.getFirstName())
                .lastName(b.getLastName())
                .dateOfBirth(b.getDateOfBirth())
                .gender(b.getGender())
                .village(b.getVillage())
                .taluka(b.getTaluka())
                .district(b.getDistrict())
                .state(b.getState())
                .pinCode(b.getPinCode())
                .annualIncome(b.getAnnualIncome())
                .landHolding(b.getLandHolding())
                .category(b.getCategory())
                .registrationStatus(b.getRegistrationStatus())
                .identityVerified(b.isIdentityVerified())
                .emailVerified(b.isEmailVerified())
                .build();
    }

    private DocumentResponse mapToDocumentResponse(
            BeneficiaryDocument d) {

        return DocumentResponse.builder()
                .id(d.getId())
                .beneficiaryId(
                        d.getBeneficiary().getId().longValue()
                )
                .applicationId(
                        d.getApplicationId()
                )
                .stageNumber(
                        d.getStageNumber()
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
