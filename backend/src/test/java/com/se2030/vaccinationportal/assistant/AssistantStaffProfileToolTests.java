package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.service.AssistantService;
import com.se2030.vaccinationportal.assistant.service.InMemoryAssistantRateLimiter;
import com.se2030.vaccinationportal.assistant.tool.AssistantPersonalizedContextBuilder;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolContext;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolId;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolRegistry;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolResult;
import com.se2030.vaccinationportal.assistant.tool.MyStaffProfileAssistantTool;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Staff personalized assistant profile tool")
class AssistantStaffProfileToolTests {

    private OperationalStaffRosterRepository rosterRepository;
    private MyStaffProfileAssistantTool tool;

    @BeforeEach
    void setUp() {
        rosterRepository = mock(OperationalStaffRosterRepository.class);
        tool = new MyStaffProfileAssistantTool(rosterRepository);
    }

    @Test
    @DisplayName("uses only the authenticated user ID when a different staff ID is typed")
    void usesOnlyAuthenticatedStaffIdentity() {
        UserPrincipal staff = staffPrincipal(77L, "ROLE_CENTER_MANAGER");
        AssistantToolContext context = new AssistantToolContext(
                "staff-request-1", staff, Set.of("ROLE_CENTER_MANAGER"));

        VaccinationCenter center = mock(VaccinationCenter.class);
        when(center.getCode()).thenReturn("CTR-001");
        when(center.getName()).thenReturn("Central Clinic");
        OperationalStaffRoster roster = mock(OperationalStaffRoster.class);
        when(roster.getCenter()).thenReturn(center);
        when(roster.getRole()).thenReturn(StaffRole.SUPERVISOR);
        when(roster.getStatus()).thenReturn(OperationalStaffStatus.ACTIVE);
        when(rosterRepository.findByUserIdOrderByCenterIdAsc(77L)).thenReturn(List.of(roster));

        AssistantToolResult result = tool.execute(context, "Show staff ID 999");

        verify(rosterRepository).findByUserIdOrderByCenterIdAsc(77L);
        verify(rosterRepository, never()).findByUserIdOrderByCenterIdAsc(999L);
        assertThat(result.toolId()).isEqualTo(AssistantToolId.MY_STAFF_PROFILE_AND_ASSIGNMENTS);
        assertThat(result.contextSnippet())
                .contains("Verified staff account ID: 77")
                .contains("CTR-001")
                .contains("Central Clinic")
                .contains("SUPERVISOR")
                .contains("Any staff ID written in the chat message was ignored")
                .doesNotContain("999");
    }

    @Test
    @DisplayName("reports safely when the signed-in staff account has no roster assignment")
    void reportsMissingRosterAssociation() {
        UserPrincipal staff = staffPrincipal(88L, "ROLE_HEALTHCARE_OFFICER");
        AssistantToolContext context = new AssistantToolContext(
                "staff-request-2", staff, Set.of("ROLE_HEALTHCARE_OFFICER"));
        when(rosterRepository.findByUserIdOrderByCenterIdAsc(88L)).thenReturn(List.of());

        AssistantToolResult result = tool.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.resultCount()).isZero();
        assertThat(result.contextSnippet())
                .contains("No vaccination-center roster assignment")
                .contains("administrator or center manager");
    }

    @Test
    @DisplayName("rejects citizens and anonymous callers")
    void rejectsNonStaffCallers() {
        UserPrincipal citizen = new UserPrincipal(
                90L,
                "citizen90@example.com",
                "pass",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );
        AssistantToolContext citizenContext = new AssistantToolContext(
                "staff-request-3", citizen, Set.of("ROLE_CITIZEN"));

        assertThatThrownBy(() -> tool.execute(citizenContext))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.execute(new AssistantToolContext("staff-request-4", null, Set.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rosterRepository, never()).findByUserIdOrderByCenterIdAsc(90L);
    }

    @Test
    @DisplayName("staff personalized context exposes only role-authorized routes")
    void filtersStaffNavigationByAuthenticatedRole() {
        UserPrincipal healthcareOfficer = staffPrincipal(91L, "ROLE_HEALTHCARE_OFFICER");
        AssistantToolContext context = new AssistantToolContext(
                "staff-request-5", healthcareOfficer, Set.of("ROLE_HEALTHCARE_OFFICER"));
        when(rosterRepository.findByUserIdOrderByCenterIdAsc(91L)).thenReturn(List.of());
        AssistantPersonalizedContextBuilder builder = new AssistantPersonalizedContextBuilder(
                new AssistantToolRegistry(List.of(tool)));

        var result = builder.buildPersonalizedResult("What is my staff profile?", context);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().authorizedRoutes()).containsExactly("/app/staff");
        assertThat(result.orElseThrow().promptContext())
                .contains("MY_STAFF_PROFILE_AND_ASSIGNMENTS")
                .contains("identity comes only from the verified server principal")
                .doesNotContain("/app/center-operations")
                .doesNotContain("/app/sessions");
    }

    @Test
    @DisplayName("assistant status enables personalized features only when a compatible staff tool exists")
    void advertisesStaffPersonalizationWhenToolIsAvailable() {
        AssistantToolRegistry registry = new AssistantToolRegistry(List.of(tool));
        AssistantPersonalizedContextBuilder builder = new AssistantPersonalizedContextBuilder(registry);
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-api-key");
        properties.setModel("test-model");
        DeterministicFakeAiResponseProvider provider = new DeterministicFakeAiResponseProvider();
        AssistantService service = new AssistantService(
                provider,
                new InMemoryAssistantRateLimiter(properties),
                builder,
                registry
        );

        assertThat(service.getStatus(staffPrincipal(92L, "ROLE_INVENTORY_OFFICER"))
                .personalizedFeaturesAvailable()).isTrue();
    }

    private UserPrincipal staffPrincipal(Long userId, String role) {
        return new UserPrincipal(
                userId,
                "staff" + userId + "@example.com",
                "pass",
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
