package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a document uploaded for a beneficiary.
 *
 * <p>Maps to the {@code beneficiary_documents} table. Stores document
 * metadata (type, file name, storage path, size, MIME type) while the
 * actual binary file is stored on the local filesystem under
 * {@code app.storage.upload-dir/beneficiary/{beneficiaryId}/}.
 *
 * <p>Has a {@code ManyToOne} relationship with {@link Beneficiary}.
 * Cascades delete (at DB level via FK constraint ON DELETE CASCADE).
 */
@Entity
@Table(name = "beneficiary_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_doc_beneficiary"))
    private Beneficiary beneficiary;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    /** UUID-prefixed stored filename on the filesystem. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Original filename as uploaded by the officer. */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /** Absolute path to the file on the server filesystem. */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    /** {@code true} once a Field Officer has manually verified the document. */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
