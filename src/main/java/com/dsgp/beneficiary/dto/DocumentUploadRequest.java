package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for document upload operations.
 *
 * <p>The actual file binary is received as {@code MultipartFile} in the
 * controller. This DTO carries the document type metadata and the uploader
 * identity, bound from the multipart request parameters.
 */
@Data
public class DocumentUploadRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    /** Name or ID of the officer performing the upload. */
    private String uploadedBy;
}
