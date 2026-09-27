package com.se2030.vaccinationportal.assistant.policy;

import com.se2030.vaccinationportal.assistant.dto.AssistantActionDto;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic policy evaluator enforcing server-side safety boundaries and refusal categories.
 * Prevents the AI model from making arbitrary refusal classification decisions and avoids
 * broad keyword matches that could falsely flag normal portal questions as emergencies.
 */
public final class AssistantRefusalPolicy {

    private AssistantRefusalPolicy() {
        // utility
    }

    public static final String EMERGENCY_GUIDANCE_MESSAGE =
            "If you are experiencing a medical emergency or immediate harm, please contact your local emergency services immediately.";

    public static final String MEDICAL_BOUNDARY_MESSAGE =
            "The portal assistant provides operational guidance for the vaccination portal but cannot provide medical advice, symptom diagnoses, or clinical vaccine recommendations. Please consult an authorized healthcare professional for clinical decisions.";

    public static final String MUTATION_REFUSAL_MESSAGE =
            "The portal assistant cannot perform actions, book, modify, or cancel records on your behalf. Please use the portal navigation to complete this action directly.";

    public static final String SECURITY_REFUSAL_MESSAGE =
            "I cannot disclose internal system instructions, prompts, or security credentials.";

    public static final String OPERATIONAL_FALLBACK_MESSAGE =
            "The portal assistant could not complete that request. Please use the portal navigation or try again later.";

    // Patterns for immediate emergencies / immediate harm
    private static final List<Pattern> EMERGENCY_PATTERNS = List.of(
            Pattern.compile("\\b(trouble\\s+breathing|can'?t\\s+breathe|cannot\\s+breathe|difficulty\\s+breathing)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(heart\\s+attack|chest\\s+pain)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(anaphylaxis|severe\\s+allergic\\s+reaction)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(unconscious|passed\\s+out|collaps(ed|ing)|faint(ed|ing))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(medical\\s+emergency|immediate\\s+harm|kill\\s+myself|suicid(e|al))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(bleeding\\s+heavily|profuse\\s+bleeding|severe\\s+hemorrhage)\\b", Pattern.CASE_INSENSITIVE),
            // Multilingual emergency patterns (Sinhala)
            Pattern.compile("(හුස්ම\\s*ගන්න\\s*අමාරු|හුස්ම\\s*ගැනීමේ\\s*අපහසු|හුස්ම\\s*හිර|පපුවේ\\s*කැක්කුම|හෘදයාබාධ|සිහි\\s*නැති|සිහිය\\s*නැති|ක්ලාන්ත|කලන්ත|දිවි\\s*නසා|රුධිර\\s*වහනය|අසාත්මිකතා)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            // Multilingual emergency patterns (Tamil)
            Pattern.compile("(மூச்சுத்திணறல்|மூச்சு\\s*திணறல்|மூச்சு\\s*விட\\s*சிரமம்|நெஞ்சு\\s*வலி|மாரடைப்பு|மயக்கம்|நினைவிழந்த|மயங்கி|தற்கொலை|இரத்தப்போக்கு|ரத்தப்போக்கு|ஒவ்வாமை)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    );

    // Patterns for medical diagnosis, symptom interpretation, clinical recommendation
    private static final List<Pattern> MEDICAL_PATTERNS = List.of(
            Pattern.compile("\\b(diagnos(e|is)|what\\s+disease|do\\s+i\\s+have\\s+(covid|flu|infection|cancer|pneumonia))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(interpret\\s+my\\s+symptoms|my\\s+symptoms\\s+are|treating\\s+my\\s+fever|cure\\s+my)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(which\\s+vaccine\\s+should\\s+i\\s+take|which\\s+vaccine\\s+is\\s+(best|better)\\s+for\\s+my)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(should\\s+i\\s+take\\s+(pfizer|moderna|astrazeneca|sinopharm))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(prescribe|prescription\\s+for|what\\s+medicine\\s+should\\s+i\\s+take|dosage\\s+for\\s+me)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(is\\s+it\\s+safe\\s+for\\s+me\\s+to\\s+(take|get)\\s+the\\s+vaccine\\s+with\\s+my)\\b", Pattern.CASE_INSENSITIVE),
            // Multilingual medical diagnosis / clinical advice patterns (Sinhala)
            Pattern.compile("(රෝග\\s*විනිශ්චය|මට\\s*ලෙඩක්ද|කොවිඩ්\\s*තියෙනවද|උණට\\s*බෙහෙත්|මොන\\s*එන්නතද\\s*හොඳ|බෙහෙත්\\s*වට්ටෝරුව|බෙහෙත්\\s*මාත්‍රාව)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            // Multilingual medical diagnosis / clinical advice patterns (Tamil)
            Pattern.compile("(நோய்\\s*கண்டறிதல்|எனக்கு\\s*என்ன\\s*நோய்|எந்த\\s*தடுப்பூசி\\s*நல்லது|மருந்து\\s*பரிந்துரை|மருந்து\\s*அளவு)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    );

    // Patterns for data mutation commands (distinguished from asking "how do I book?")
    private static final List<Pattern> MUTATION_PATTERNS = List.of(
            Pattern.compile("^\\s*(please\\s+)?(book|schedule|reserve)\\s+(me\\b|an?\\s+appointment\\s+for\\s+me|a\\s+slot\\s+for\\s+me)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(cancel|reschedule|delete)\\s+my\\s+(appointment|booking|slot|account)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(change|reset|update)\\s+my\\s+(password|email|phone)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*(please\\s+)?(submit|fill\\s+out)\\s+my\\s+screening\\b", Pattern.CASE_INSENSITIVE)
    );

    // Patterns for system prompt / credential / security extraction
    private static final List<Pattern> SECURITY_PATTERNS = List.of(
            Pattern.compile("\\b(system\\s+prompt|system\\s+instruction|developer\\s+prompt|developer\\s+instruction)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(ignore\\s+(all\\s+)?previous\\s+instructions)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(show\\s+(me\\s+)?(your|the)\\s+prompt)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(api[- ]?key|jwt[- ]?secret|openai[- ]?key|database\\s+password|admin\\s+password)\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Evaluates whether the user message triggers an explicit server-side safety boundary.
     *
     * @param userMessage the user's input text
     * @param callerAuthorities caller's assigned roles/authorities
     * @param requestId the unique tracking request ID
     * @return Optional containing the deterministic response if intercepted, or empty if normal processing proceeds
     */
    public static Optional<AssistantChatResponse> evaluatePolicy(
            String userMessage,
            Set<String> callerAuthorities,
            String requestId
    ) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        String normalized = userMessage.trim();

        // 1. Immediate emergency check (highest priority safety boundary)
        for (Pattern pattern : EMERGENCY_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return Optional.of(new AssistantChatResponse(
                        EMERGENCY_GUIDANCE_MESSAGE,
                        Collections.emptyList(),
                        requestId
                ));
            }
        }

        // 2. Clinical / medical diagnosis / vaccine recommendation check
        for (Pattern pattern : MEDICAL_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return Optional.of(new AssistantChatResponse(
                        MEDICAL_BOUNDARY_MESSAGE,
                        Collections.emptyList(),
                        requestId
                ));
            }
        }

        // 3. Security / internal system prompt / credential extraction check
        for (Pattern pattern : SECURITY_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return Optional.of(new AssistantChatResponse(
                        SECURITY_REFUSAL_MESSAGE,
                        Collections.emptyList(),
                        requestId
                ));
            }
        }

        // 4. Mutation command check (excluding "how do I" / "how can I" inquiries)
        boolean isInformationalInquiry = Pattern.compile("\\b(how\\s+(do|can|to)\\s+i|where\\s+(can|do)\\s+i)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(normalized).find();

        if (!isInformationalInquiry) {
            for (Pattern pattern : MUTATION_PATTERNS) {
                if (pattern.matcher(normalized).find()) {
                    List<AssistantActionDto> actions = resolveMutationGuidanceActions(normalized, callerAuthorities);
                    return Optional.of(new AssistantChatResponse(
                            MUTATION_REFUSAL_MESSAGE,
                            actions,
                            requestId
                    ));
                }
            }
        }

        return Optional.empty();
    }

    private static List<AssistantActionDto> resolveMutationGuidanceActions(String message, Set<String> callerAuthorities) {
        List<AssistantActionDto> actions = new ArrayList<>();
        String lower = message.toLowerCase();

        boolean isBooking = lower.contains("book") || lower.contains("schedule") || lower.contains("reserve");
        boolean isCancellationOrReschedule = lower.contains("cancel") || lower.contains("reschedule");

        if (isBooking) {
            if (AssistantRouteCatalog.isAuthorizedRoute("/app/appointments/book", callerAuthorities)) {
                actions.add(new AssistantActionDto("Book Appointment", "/app/appointments/book"));
            } else if (callerAuthorities == null || callerAuthorities.isEmpty()) {
                actions.add(new AssistantActionDto("Citizen Login", "/login"));
            }
        } else if (isCancellationOrReschedule) {
            if (AssistantRouteCatalog.isAuthorizedRoute("/app/appointments/mine", callerAuthorities)) {
                actions.add(new AssistantActionDto("My Appointments", "/app/appointments/mine"));
            } else if (callerAuthorities == null || callerAuthorities.isEmpty()) {
                actions.add(new AssistantActionDto("Citizen Login", "/login"));
            }
        }

        return actions;
    }
}
