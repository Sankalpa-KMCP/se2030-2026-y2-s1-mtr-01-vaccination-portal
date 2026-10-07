package com.se2030.vaccinationportal.appointment.testsupport;

import com.se2030.vaccinationportal.shared.auth.api.EmailVerificationStatusReader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test stand-in for the Auth-owned email verification status contract.
 * Citizens are unverified unless explicitly marked verified, so fixtures must state
 * their citizens' verification state explicitly instead of inheriting a silent default.
 * Contains no real business logic — do not reuse in production code.
 */
public class TestEmailVerificationStatusReaderDouble implements EmailVerificationStatusReader {

    private final Set<Long> verifiedCitizenIds = ConcurrentHashMap.newKeySet();

    public void markVerified(Long... citizenIds) {
        for (Long citizenId : citizenIds) {
            verifiedCitizenIds.add(citizenId);
        }
    }

    public void markUnverified(Long citizenId) {
        verifiedCitizenIds.remove(citizenId);
    }

    public void clear() {
        verifiedCitizenIds.clear();
    }

    @Override
    public boolean isEmailVerified(Long userId) {
        return userId != null && verifiedCitizenIds.contains(userId);
    }
}
