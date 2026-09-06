package com.dsgp.location;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
@CrossOrigin(origins = "http://localhost:5173")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/states")
    public List<State> getStates() {
        return locationService.getAllStates();
    }

    @GetMapping("/districts/{stateId}")
    public List<District> getDistricts(@PathVariable Integer stateId) {
        return locationService.getDistrictsByState(stateId);
    }

    @GetMapping("/talukas/{districtId}")
    public List<Taluka> getTalukas(@PathVariable Integer districtId) {
        return locationService.getTalukasByDistrict(districtId);
    }

    @GetMapping("/villages/{talukaId}")
    public List<Village> getVillages(@PathVariable Integer talukaId) {
        return locationService.getVillagesByTaluka(talukaId);
    }
}