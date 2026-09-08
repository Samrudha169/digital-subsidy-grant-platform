package com.dsgp.authentication.repository;

import com.dsgp.authentication.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Officer} entities.
 */
@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    /** Find an officer by their login username. */
    Optional<Officer> findByUsername(String username);

    /** Check whether a username is already taken. */
    boolean existsByUsername(String username);
}
