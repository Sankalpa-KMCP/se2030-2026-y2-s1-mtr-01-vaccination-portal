package com.se2030.vaccinationportal.appointment.api.query;

public record WaitlistSummary(String status, Long campaignId, Long sessionId, Integer queuePosition) {
}
