package com.dsgp.verification.repository;

import com.dsgp.verification.entity.VerificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationRecordRepository
        extends JpaRepository<VerificationRecord, Long> {

    List<VerificationRecord> findBySchemeApplicationIdOrderByPerformedAtAsc(
            Long applicationId
    );
}