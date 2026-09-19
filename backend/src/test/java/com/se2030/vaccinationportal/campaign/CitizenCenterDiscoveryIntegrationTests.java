package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for citizen vaccination center discovery (GET /api/v1/citizen/centers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CitizenCenterDiscoveryIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationCenterRepository centerRepository;

    @BeforeEach
    void setUp() {
        centerRepository.deleteAll();
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    private VaccinationCenter createCenter(String code, String name, CenterStatus status) {
        VaccinationCenter center = new VaccinationCenter(
                code,
                name,
                "123 Test Street",
                "+94112345678",
                status,
                50,
                100,
                "Asia/Colombo"
        );
        return centerRepository.save(center);
    }

    @Test
    @DisplayName("Anonymous GET /api/v1/citizen/centers is denied 403")
    void anonymousRequestDenied() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/centers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated ROLE_CITIZEN succeeds with 200 and extracts active centers with code and id")
    void citizenCanDiscoverActiveCenters() throws Exception {
        VaccinationCenter activeCenter = createCenter("CTR-COL-01", "Colombo Central Clinic", CenterStatus.ACTIVE);
        createCenter("CTR-INACT-02", "Inactive Clinic", CenterStatus.INACTIVE);

        String auth = token(2L, "citizen@example.com", List.of("ROLE_CITIZEN"));

        mockMvc.perform(get("/api/v1/citizen/centers")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(activeCenter.getId()))
                .andExpect(jsonPath("$[0].code").value("CTR-COL-01"))
                .andExpect(jsonPath("$[0].name").value("Colombo Central Clinic"))
                .andExpect(jsonPath("$[0].timezone").value("Asia/Colombo"));
    }
}
