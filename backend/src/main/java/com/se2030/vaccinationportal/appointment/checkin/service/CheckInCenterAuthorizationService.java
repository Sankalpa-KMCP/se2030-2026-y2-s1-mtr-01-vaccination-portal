package com.se2030.vaccinationportal.appointment.checkin.service;

import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces center ownership for MF-03 operational queue and check-in actions. */
@Service
public class CheckInCenterAuthorizationService {

    private final AppointmentRepository appointments;
    private final SlotCapacityPort slots;
    private final StaffContextQuery staffContext;

    public CheckInCenterAuthorizationService(
            AppointmentRepository appointments,
            @Qualifier("timeSlotCapacityService") SlotCapacityPort slots,
            StaffContextQuery staffContext
    ) {
        this.appointments = appointments;
        this.slots = slots;
        this.staffContext = staffContext;
    }

    @Transactional(readOnly = true)
    public void requireAppointmentAccess(UserPrincipal principal, Long appointmentId) {
        Long slotId = appointments.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found with ID: " + appointmentId))
                .getSlotId();
        requireSlotAccess(principal, slotId);
    }

    @Transactional(readOnly = true)
    public void requireSlotAccess(UserPrincipal principal, Long slotId) {
        if (principal == null) {
            throw new AccessDeniedException("Authenticated staff principal is required");
        }
        if (hasGlobalAccess(principal)) {
            return;
        }
        Long centerId = slots.getCenterIdForSlot(slotId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unable to resolve a vaccination center for slot " + slotId));
        if (!staffContext.isActiveAtCenter(principal, centerId)) {
            throw new AccessDeniedException(
                    "Staff member is not active at the vaccination center for slot " + slotId);
        }
    }

    private static boolean hasGlobalAccess(UserPrincipal principal) {
        return principal.hasRole("ROLE_ADMIN") || principal.hasRole("ADMIN")
                || principal.hasRole("ROLE_SUPER_ADMIN") || principal.hasRole("SUPER_ADMIN");
    }
}
