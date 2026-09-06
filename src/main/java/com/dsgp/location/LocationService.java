package com.dsgp.location;

import java.util.List;

public interface LocationService {

    List<State> getAllStates();

    List<District> getDistrictsByState(Integer stateId);

    List<Taluka> getTalukasByDistrict(Integer districtId);

    List<Village> getVillagesByTaluka(Integer talukaId);
}