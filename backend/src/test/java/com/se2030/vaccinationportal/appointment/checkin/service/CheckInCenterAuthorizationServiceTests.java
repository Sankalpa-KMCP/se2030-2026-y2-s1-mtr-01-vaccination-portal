package com.se2030.vaccinationportal.appointment.checkin.service;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckInCenterAuthorizationServiceTests {

    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final SlotCapacityPort slots = mock(SlotCapacityPort.class);
    private final StaffContextQuery staff = mock(StaffContextQuery.class);
    private final CheckInCenterAuthorizationService service =
            new CheckInCenterAuthorizationService(appointments, slots, staff);

    @Test
    void centerManagerMustHaveActiveRosterAtSlotCenter() {
        UserPrincipal manager = principal(41L, "ROLE_CENTER_MANAGER");
        when(slots.getCenterIdForSlot(4001L)).thenReturn(Optional.of(501L));
        when(staff.isActiveAtCenter(manager, 501L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.requireSlotAccess(manager, 4001L));

        when(staff.isActiveAtCenter(manager, 501L)).thenReturn(true);
        assertDoesNotThrow(() -> service.requireSlotAccess(manager, 4001L));
    }

    @Test
    void appointmentAuthorizationUsesItsAuthoritativeSlot() {
        UserPrincipal vaccinator = principal(42L, "ROLE_VACCINATOR");
        when(appointments.findById(77L)).thenReturn(Optional.of(new Appointment(1L, 2L, 4002L)));
        when(slots.getSessionIdForSlot(4002L)).thenReturn(Optional.of(602L));
        when(staff.isAssignedToSession(org.mockito.ArgumentMatchers.eq(vaccinator),
                org.mockito.ArgumentMatchers.eq(602L), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(true);

        assertDoesNotThrow(() -> service.requireAppointmentAccess(vaccinator, 77L));
        verify(staff).isAssignedToSession(org.mockito.ArgumentMatchers.eq(vaccinator),
                org.mockito.ArgumentMatchers.eq(602L), org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void vaccinatorCannotReadQueueForUnassignedSession() {
        UserPrincipal vaccinator = principal(42L, "ROLE_VACCINATOR");
        when(slots.getSessionIdForSlot(4003L)).thenReturn(Optional.of(603L));
        when(staff.isAssignedToSession(org.mockito.ArgumentMatchers.eq(vaccinator),
                org.mockito.ArgumentMatchers.eq(603L), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.requireSlotAccess(vaccinator, 4003L));
    }

    @Test
    void administratorHasGlobalAccessWithoutRosterLookup() {
        UserPrincipal admin = principal(9L, "ROLE_ADMIN");

        assertDoesNotThrow(() -> service.requireSlotAccess(admin, 9999L));

        verify(slots, never()).getCenterIdForSlot(9999L);
    }

    private static UserPrincipal principal(Long userId, String role) {
        return new UserPrincipal(userId, "staff@portal.test", null, true,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
