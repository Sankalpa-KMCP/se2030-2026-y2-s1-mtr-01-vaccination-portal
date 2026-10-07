package com.se2030.vaccinationportal.campaign.api.staff;

import java.util.List;

public record StaffProfileSummary(int totalAssignments, List<StaffRosterSummary> roster) {
    public StaffProfileSummary {
        roster = List.copyOf(roster);
    }
}
