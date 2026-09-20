package com.dsgp.location;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Unit tests for {@link LocationServiceImpl}.
 *
 * <p>All repository interactions are mocked.  Tests verify service-layer
 * delegation and the correctness of each repository query method invoked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationServiceImpl")
class LocationServiceImplTest {

    @Mock
    private StateRepository stateRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private TalukaRepository talukaRepository;

    @Mock
    private VillageRepository villageRepository;

    @InjectMocks
    private LocationServiceImpl locationService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private State state(int id, String name) {
        State s = new State();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private District district(int id, String name) {
        District d = new District();
        d.setId(id);
        d.setName(name);
        return d;
    }

    private Taluka taluka(int id, String name) {
        Taluka t = new Taluka();
        t.setId(id);
        t.setName(name);
        return t;
    }

    private Village village(int id, String name) {
        Village v = new Village();
        v.setId(id);
        v.setName(name);
        return v;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getAllStates
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllStates")
    class GetAllStates {

        @Test
        @DisplayName("returns every state returned by the repository")
        void returnsAllStates() {

            List<State> expected = List.of(
                    state(1, "Maharashtra"),
                    state(2, "Andhra Pradesh"),
                    state(3, "Arunachal Pradesh"),
                    state(4, "Assam"),
                    state(5, "Bihar"),
                    state(6, "Chhattisgarh"),
                    state(7, "Goa"),
                    state(8, "Gujarat"),
                    state(9, "Haryana"),
                    state(10, "Himachal Pradesh"),
                    state(11, "Jharkhand"),
                    state(12, "Karnataka"),
                    state(13, "Kerala"),
                    state(14, "Madhya Pradesh"),
                    state(15, "Manipur"),
                    state(16, "Meghalaya"),
                    state(17, "Mizoram"),
                    state(18, "Nagaland"),
                    state(19, "Odisha"),
                    state(20, "Punjab"),
                    state(21, "Rajasthan"),
                    state(22, "Sikkim"),
                    state(23, "Tamil Nadu"),
                    state(24, "Telangana"),
                    state(25, "Tripura"),
                    state(26, "Uttar Pradesh"),
                    state(27, "Uttarakhand"),
                    state(28, "West Bengal"),
                    state(29, "Andaman and Nicobar Islands"),
                    state(30, "Chandigarh"),
                    state(31, "Dadra and Nagar Haveli and Daman and Diu"),
                    state(32, "Delhi"),
                    state(33, "Jammu and Kashmir"),
                    state(34, "Ladakh"),
                    state(35, "Lakshadweep"),
                    state(36, "Puducherry")
            );

            given(stateRepository.findAll()).willReturn(expected);

            List<State> result = locationService.getAllStates();

            assertThat(result).hasSize(36);
            assertThat(result).isEqualTo(expected);

            then(stateRepository).should().findAll();
        }

        @Test
        @DisplayName("returns 36 entries covering all 28 states and 8 Union Territories")
        void covers28StatesAnd8UnionTerritories() {

            // Build a representative list with the correct count and spot-check names
            List<State> all36 = List.of(
                    state(1,  "Maharashtra"),
                    state(2,  "Andhra Pradesh"),
                    state(3,  "Arunachal Pradesh"),
                    state(4,  "Assam"),
                    state(5,  "Bihar"),
                    state(6,  "Chhattisgarh"),
                    state(7,  "Goa"),
                    state(8,  "Gujarat"),
                    state(9,  "Haryana"),
                    state(10, "Himachal Pradesh"),
                    state(11, "Jharkhand"),
                    state(12, "Karnataka"),
                    state(13, "Kerala"),
                    state(14, "Madhya Pradesh"),
                    state(15, "Manipur"),
                    state(16, "Meghalaya"),
                    state(17, "Mizoram"),
                    state(18, "Nagaland"),
                    state(19, "Odisha"),
                    state(20, "Punjab"),
                    state(21, "Rajasthan"),
                    state(22, "Sikkim"),
                    state(23, "Tamil Nadu"),
                    state(24, "Telangana"),
                    state(25, "Tripura"),
                    state(26, "Uttar Pradesh"),
                    state(27, "Uttarakhand"),
                    state(28, "West Bengal"),
                    state(29, "Andaman and Nicobar Islands"),
                    state(30, "Chandigarh"),
                    state(31, "Dadra and Nagar Haveli and Daman and Diu"),
                    state(32, "Delhi"),
                    state(33, "Jammu and Kashmir"),
                    state(34, "Ladakh"),
                    state(35, "Lakshadweep"),
                    state(36, "Puducherry")
            );

            given(stateRepository.findAll()).willReturn(all36);

            List<State> result = locationService.getAllStates();

            List<String> names = result.stream().map(State::getName).toList();

            // Total count
            assertThat(result).hasSize(36);

            // Maharashtra preserved at index 0 (id=1)
            assertThat(result.get(0).getName()).isEqualTo("Maharashtra");
            assertThat(result.get(0).getId()).isEqualTo(1);

            // All 28 states present
            assertThat(names).contains(
                    "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar",
                    "Chhattisgarh", "Goa", "Gujarat", "Haryana",
                    "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
                    "Madhya Pradesh", "Manipur", "Meghalaya", "Mizoram",
                    "Nagaland", "Odisha", "Punjab", "Rajasthan",
                    "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
                    "Uttar Pradesh", "Uttarakhand", "West Bengal"
            );

            // All 8 Union Territories present
            assertThat(names).contains(
                    "Andaman and Nicobar Islands",
                    "Chandigarh",
                    "Dadra and Nagar Haveli and Daman and Diu",
                    "Delhi",
                    "Jammu and Kashmir",
                    "Ladakh",
                    "Lakshadweep",
                    "Puducherry"
            );
        }

        @Test
        @DisplayName("returns empty list when no states are seeded")
        void returnsEmptyListWhenNoStates() {

            given(stateRepository.findAll()).willReturn(List.of());

            List<State> result = locationService.getAllStates();

            assertThat(result).isEmpty();
            then(stateRepository).should().findAll();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getDistrictsByState
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDistrictsByState")
    class GetDistrictsByState {

        @Test
        @DisplayName("delegates to districtRepository.findByStateId")
        void delegatesToRepository() {

            List<District> expected = List.of(district(1, "Pune"));
            given(districtRepository.findByStateId(1)).willReturn(expected);

            List<District> result = locationService.getDistrictsByState(1);

            assertThat(result).isEqualTo(expected);
            then(districtRepository).should().findByStateId(1);
        }

        @Test
        @DisplayName("returns empty list for a state with no districts seeded")
        void returnsEmptyForStateWithNoDistricts() {

            given(districtRepository.findByStateId(2)).willReturn(List.of());

            List<District> result = locationService.getDistrictsByState(2);

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getTalukasByDistrict
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTalukasByDistrict")
    class GetTalukasByDistrict {

        @Test
        @DisplayName("delegates to talukaRepository.findByDistrictId")
        void delegatesToRepository() {

            List<Taluka> expected = List.of(taluka(1, "Haveli"));
            given(talukaRepository.findByDistrictId(1)).willReturn(expected);

            List<Taluka> result = locationService.getTalukasByDistrict(1);

            assertThat(result).isEqualTo(expected);
            then(talukaRepository).should().findByDistrictId(1);
        }

        @Test
        @DisplayName("returns empty list for a district with no talukas seeded")
        void returnsEmptyForDistrictWithNoTalukas() {

            given(talukaRepository.findByDistrictId(99)).willReturn(List.of());

            List<Taluka> result = locationService.getTalukasByDistrict(99);

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getVillagesByTaluka
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getVillagesByTaluka")
    class GetVillagesByTaluka {

        @Test
        @DisplayName("delegates to villageRepository.findByTalukaId")
        void delegatesToRepository() {

            List<Village> expected = List.of(village(1, "Pune"));
            given(villageRepository.findByTalukaId(1)).willReturn(expected);

            List<Village> result = locationService.getVillagesByTaluka(1);

            assertThat(result).isEqualTo(expected);
            then(villageRepository).should().findByTalukaId(1);
        }

        @Test
        @DisplayName("returns empty list for a taluka with no villages seeded")
        void returnsEmptyForTalukaWithNoVillages() {

            given(villageRepository.findByTalukaId(99)).willReturn(List.of());

            List<Village> result = locationService.getVillagesByTaluka(99);

            assertThat(result).isEmpty();
        }
    }
}
