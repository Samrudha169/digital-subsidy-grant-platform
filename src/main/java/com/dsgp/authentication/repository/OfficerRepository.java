package com.dsgp.authentication.repository;

import com.dsgp.authentication.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    Optional<Officer> findByUsername(String username);

    Optional<Officer> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}