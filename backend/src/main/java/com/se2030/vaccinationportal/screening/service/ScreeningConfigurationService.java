package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.Questionnaire;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing ScreeningQuestion catalog, AnswerOptions, and root Questionnaires.
 * Enforces safe CRUD: prevents hard delete if referenced by published versions or answers.
 */
@Service
@Transactional
public class ScreeningConfigurationService {

    private final ScreeningQuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireMutabilityValidator mutabilityValidator;

    public ScreeningConfigurationService(
            ScreeningQuestionRepository questionRepository,
            AnswerOptionRepository answerOptionRepository,
            QuestionnaireRepository questionnaireRepository,
            QuestionnaireMutabilityValidator mutabilityValidator
    ) {
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.mutabilityValidator = mutabilityValidator;
    }

    // ==========================================
    // Screening Question Operations
    // ==========================================

    @Transactional(readOnly = true)
    public List<ScreeningQuestion> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ScreeningQuestion> getActiveQuestions() {
        return questionRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public ScreeningQuestion getQuestionById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Question ID cannot be null");
        }
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Screening question not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public ScreeningQuestion getQuestionByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Question code cannot be blank");
        }
        return questionRepository.findByCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Screening question not found with code: " + code));
    }

    public ScreeningQuestion createQuestion(String code, String text, String description, QuestionType type, boolean defaultRequired) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Question code is required");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Question type is required");
        }
        if (questionRepository.existsByCode(code.trim())) {
            throw new IllegalStateException("Screening question with code '" + code.trim() + "' already exists");
        }

        ScreeningQuestion question = new ScreeningQuestion(code.trim(), text.trim(), type, defaultRequired, true);
        question.setDescription(description != null ? description.trim() : null);
        return questionRepository.save(question);
    }

    public ScreeningQuestion updateQuestion(Long id, String text, String description, boolean defaultRequired, boolean active) {
        ScreeningQuestion question = getQuestionById(id);
        mutabilityValidator.assertQuestionCanBeUpdated(id);
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text is required");
        }
        question.setText(text.trim());
        question.setDescription(description != null ? description.trim() : null);
        question.setDefaultRequired(defaultRequired);
        question.setActive(active);
        return questionRepository.save(question);
    }

    public void deleteQuestion(Long id) {
        ScreeningQuestion question = getQuestionById(id);
        mutabilityValidator.assertQuestionCanBeDeleted(id);
        questionRepository.delete(question);
    }

    // ==========================================
    // Answer Option Operations
    // ==========================================

    @Transactional(readOnly = true)
    public List<AnswerOption> getOptionsForQuestion(Long questionId) {
        getQuestionById(questionId);
        return answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
    }

    @Transactional(readOnly = true)
    public AnswerOption getAnswerOptionById(Long optionId) {
        if (optionId == null) {
            throw new IllegalArgumentException("Answer option ID cannot be null");
        }
        return answerOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Answer option not found with ID: " + optionId));
    }

    public AnswerOption addAnswerOption(Long questionId, String code, String label, String typedValue, int displayOrder) {
        ScreeningQuestion question = getQuestionById(questionId);
        mutabilityValidator.assertAnswerOptionsCanBeModified(questionId);

        if (question.getQuestionType() != QuestionType.SINGLE_CHOICE && question.getQuestionType() != QuestionType.MULTI_CHOICE) {
            throw new IllegalStateException("Answer options can only be added to SINGLE_CHOICE or MULTI_CHOICE questions");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Option code is required");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Option label is required");
        }
        if (typedValue == null || typedValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Option typed value is required");
        }
        if (answerOptionRepository.existsByQuestionIdAndCode(questionId, code.trim())) {
            throw new IllegalStateException("Option code '" + code.trim() + "' already exists for this question");
        }

        AnswerOption option = new AnswerOption(question, code.trim(), label.trim(), typedValue.trim(), displayOrder);
        return answerOptionRepository.save(option);
    }

    public AnswerOption updateAnswerOption(
            Long optionId,
            String code,
            String label,
            String typedValue,
            int displayOrder
    ) {
        AnswerOption option = getAnswerOptionById(optionId);
        mutabilityValidator.assertAnswerOptionsCanBeModified(option.getQuestion().getId());
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Option code is required");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Option label is required");
        }
        if (typedValue == null || typedValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Option typed value is required");
        }

        String normalizedCode = code.trim();
        Long questionId = option.getQuestion().getId();
        if (!option.getCode().equals(normalizedCode)
                && answerOptionRepository.existsByQuestionIdAndCode(questionId, normalizedCode)) {
            throw new IllegalStateException("Option code '" + normalizedCode + "' already exists for this question");
        }

        option.setCode(normalizedCode);
        option.setLabel(label.trim());
        option.setTypedValue(typedValue.trim());
        option.setDisplayOrder(displayOrder);
        return answerOptionRepository.save(option);
    }

    public void removeAnswerOption(Long optionId) {
        AnswerOption option = getAnswerOptionById(optionId);
        mutabilityValidator.assertAnswerOptionsCanBeModified(option.getQuestion().getId());
        answerOptionRepository.delete(option);
    }

    // ==========================================
    // Questionnaire Container Operations
    // ==========================================

    @Transactional(readOnly = true)
    public List<Questionnaire> getAllQuestionnaires() {
        return questionnaireRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Questionnaire getQuestionnaireById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Questionnaire ID cannot be null");
        }
        return questionnaireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire not found with ID: " + id));
    }

    public Questionnaire createQuestionnaire(String code, String name, String description) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Questionnaire code is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Questionnaire name is required");
        }
        if (questionnaireRepository.existsByCode(code.trim())) {
            throw new IllegalStateException("Questionnaire with code '" + code.trim() + "' already exists");
        }

        Questionnaire questionnaire = new Questionnaire(code.trim(), name.trim(), description != null ? description.trim() : null);
        return questionnaireRepository.save(questionnaire);
    }
}
