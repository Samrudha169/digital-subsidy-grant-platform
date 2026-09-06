package com.dsgp.location;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final TalukaRepository talukaRepository;
    private final VillageRepository villageRepository;

    public LocationServiceImpl(
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            TalukaRepository talukaRepository,
            VillageRepository villageRepository) {

        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.talukaRepository = talukaRepository;
        this.villageRepository = villageRepository;
    }

    @Override
    public List<State> getAllStates() {
        return stateRepository.findAll();
    }

    @Override
    public List<District> getDistrictsByState(Integer stateId) {
        return districtRepository.findByStateId(stateId);
    }

    @Override
    public List<Taluka> getTalukasByDistrict(Integer districtId) {
        return talukaRepository.findByDistrictId(districtId);
    }

    @Override
    public List<Village> getVillagesByTaluka(Integer talukaId) {
        return villageRepository.findByTalukaId(talukaId);
    }
}