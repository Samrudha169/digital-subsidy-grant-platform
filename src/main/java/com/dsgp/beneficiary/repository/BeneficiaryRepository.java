package com.dsgp.beneficiary.repository;

import com.dsgp.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Integer> {

    Optional<Beneficiary> findByGovId(String govId);

    Optional<Beneficiary> findByContact(String contact);

    boolean existsByGovId(String govId);

    boolean existsByContact(String contact);
}