package com.dsgp.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VillageRepository extends JpaRepository<Village, Integer> {

    List<Village> findByTalukaId(Integer talukaId);
}