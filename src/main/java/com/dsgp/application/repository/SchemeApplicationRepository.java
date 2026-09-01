package com.dsgp.application.repository;

import com.dsgp.application.entity.SchemeApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemeApplicationRepository
        extends JpaRepository<SchemeApplication, Long> {
}