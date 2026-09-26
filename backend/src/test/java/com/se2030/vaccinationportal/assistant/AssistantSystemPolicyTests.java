package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.policy.AssistantSystemPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AssistantSystemPolicy Boundary Tests")
class AssistantSystemPolicyTests {

    @Test
    @DisplayName("System policy contains required clinical disclaimer phrase and emergency referrals")
    void testPolicyContainsClinicalDisclaimers() {
        String baseInstructions = AssistantSystemPolicy.BASE_SYSTEM_INSTRUCTIONS;

        assertTrue(baseInstructions.contains(AssistantSystemPolicy.CLINICAL_DISCLAIMER_PHRASE),
                "Policy must explicitly require contacting an authorized healthcare professional or local emergency service");
        assertTrue(baseInstructions.contains("emergency"), "Policy must mention emergency services");
        assertTrue(baseInstructions.toLowerCase().contains("cannot diagnose"), "Policy must explicitly state it cannot diagnose");
        assertTrue(baseInstructions.toLowerCase().contains("cannot perform mutations"), "Policy must negate action completion");
    }

    @Test
    @DisplayName("System policy does not invent or contain 1900-VAX-CARE hotline")
    void testPolicyDoesNotInventFakeHotline() {
        String baseInstructions = AssistantSystemPolicy.BASE_SYSTEM_INSTRUCTIONS;
        assertFalse(baseInstructions.contains("1900-VAX-CARE"), "Policy must not contain the fake hotline 1900-VAX-CARE");
    }

    @Test
    @DisplayName("System policy requires English-only responses")
    void testPolicyRequiresEnglishOnlyResponses() {
        String baseInstructions = AssistantSystemPolicy.BASE_SYSTEM_INSTRUCTIONS;
        assertTrue(baseInstructions.contains("Reply only in clear, concise English"));
        assertTrue(baseInstructions.contains("Do not translate the response into another language"));
    }

    @Test
    @DisplayName("buildSystemInstruction includes user context without exposing secrets")
    void testBuildInstructionWithContext() {
        String anonymousInstruction = AssistantSystemPolicy.buildSystemInstruction("/app/citizen", (java.util.Set<String>) null);
        assertTrue(anonymousInstruction.contains("Anonymous visitor"));
        assertTrue(anonymousInstruction.contains("/app/citizen"));

        String authenticatedInstruction = AssistantSystemPolicy.buildSystemInstruction("/app/screening", "ROLE_CITIZEN");
        assertTrue(authenticatedInstruction.contains("ROLE_CITIZEN"));
        assertTrue(authenticatedInstruction.contains("/app/screening"));
        assertFalse(authenticatedInstruction.contains("sk-"));

        String multiRoleInstruction = AssistantSystemPolicy.buildSystemInstruction("/app/staff", java.util.Set.of("ROLE_ADMIN", "ROLE_INVENTORY_OFFICER"));
        assertTrue(multiRoleInstruction.contains("ROLE_ADMIN"));
        assertTrue(multiRoleInstruction.contains("ROLE_INVENTORY_OFFICER"));
    }

    @Test
    @DisplayName("buildSystemInstruction includes knowledge context securely")
    void testBuildInstructionWithKnowledgeContext() {
        String instruction = AssistantSystemPolicy.buildSystemInstruction(
                "/app/citizen/screening",
                java.util.Set.of("ROLE_CITIZEN"),
                "=== VERIFIED PORTAL KNOWLEDGE ===\n[Topic: Screening]\n=== END VERIFIED PORTAL KNOWLEDGE ==="
        );
        assertTrue(instruction.contains("ROLE_CITIZEN"));
        assertTrue(instruction.contains("/app/citizen/screening"));
        assertTrue(instruction.contains("=== VERIFIED PORTAL KNOWLEDGE ==="));
        assertTrue(instruction.contains("[Topic: Screening]"));
        assertTrue(instruction.contains("=== END VERIFIED PORTAL KNOWLEDGE ==="));
    }
}
