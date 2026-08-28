package com.dsgp.beneficiary.repository;

import com.dsgp.beneficiary.entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    Optional<Scheme> findBySchemeName(String schemeName);

    List<Scheme> findByActiveTrue();
}