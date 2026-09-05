package com.dsgp.verification.repository;

import com.dsgp.verification.entity.VerificationRecord;
import com.dsgp.verification.entity.VerificationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link VerificationRecord} entities.
 *
 * <p>Provides the persistence layer for the {@code verification_records} table.
 */
@Repository
public interface VerificationRepository extends JpaRepository<VerificationRecord, Long> {

    /**
     * Returns all verification records for a given application, ordered
     * chronologically (oldest first) so callers receive the full audit history.
     *
     * @param applicationId the scheme_application primary key
     */
    List<VerificationRecord> findBySchemeApplicationIdOrderByPerformedAtAsc(Long applicationId);

    /**
     * Returns all verification records for a given application at a specific stage.
     * Useful for checking whether a stage action has already been recorded.
     *
     * @param applicationId the scheme_application primary key
     * @param stage         the verification stage
     */
    List<VerificationRecord> findBySchemeApplicationIdAndStage(
            Long applicationId, VerificationStage stage);

    /**
     * Returns all records where the action was performed before a given timestamp.
     * Used by the SLA escalation scheduler to detect overdue verifications.
     *
     * @param stage       the stage to check
     * @param cutoff      records older than this timestamp are overdue
     */
    List<VerificationRecord> findByStageAndPerformedAtBefore(
            VerificationStage stage, LocalDateTime cutoff);
}
