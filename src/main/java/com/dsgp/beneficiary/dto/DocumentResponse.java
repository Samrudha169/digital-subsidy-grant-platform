package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO returned after a document is uploaded or retrieved.
 *
 * <p>Does not include the binary file content — only metadata.
 * The actual file can be retrieved via the file path if a download
 * endpoint is implemented in a later phase.
 */
@Data
@Builder
public class DocumentResponse {

    private Long id;
    private Long beneficiaryId;
    private DocumentType documentType;
    private String originalFileName;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private boolean verified;
}
