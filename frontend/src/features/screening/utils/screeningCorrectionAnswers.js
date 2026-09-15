import { QUESTION_TYPE } from '../constants/screeningConstants';

function emptyAnswer(questionType) {
  return questionType === QUESTION_TYPE.MULTI_CHOICE ? [] : '';
}

function sameIdentifier(left, right) {
  return left !== undefined && left !== null
    && right !== undefined && right !== null
    && String(left) === String(right);
}

function optionCodeFromSelection(options, selection) {
  if (!selection) return null;

  const matches = options.filter((option) => {
    if (selection.id !== undefined && selection.id !== null) {
      return sameIdentifier(option.id, selection.id)
        && (selection.code == null || option.code === selection.code)
        && (selection.typedValue == null || option.typedValue === selection.typedValue);
    }

    return selection.code != null
      && selection.typedValue != null
      && option.code === selection.code
      && option.typedValue === selection.typedValue;
  });

  return matches.length === 1 ? matches[0].code : null;
}

function optionCodeFromTypedValue(options, typedValue) {
  const matches = options.filter((option) => option.typedValue === typedValue);
  return matches.length === 1 ? matches[0].code : null;
}

function legacySelectionValue(selection) {
  return selection?.typedValue ?? selection?.code ?? null;
}

function choiceTokens(typedAnswer) {
  if (typedAnswer === undefined || typedAnswer === null || typedAnswer === '') return [];
  return String(typedAnswer).split(',').map((value) => value.trim()).filter(Boolean);
}

function initialSingleChoice(question, currentAnswer) {
  const options = question.options || [];
  const selectedOptions = Array.isArray(currentAnswer?.selectedOptions)
    ? currentAnswer.selectedOptions
    : [];

  if (selectedOptions.length > 0) {
    const code = optionCodeFromSelection(options, selectedOptions[0]);
    if (code) return { value: code, legacyValues: [] };

    return {
      value: '',
      legacyValues: [currentAnswer?.typedAnswer ?? legacySelectionValue(selectedOptions[0])].filter(Boolean),
    };
  }

  const typedAnswer = currentAnswer?.typedAnswer;
  if (typedAnswer === undefined || typedAnswer === null || typedAnswer === '') {
    return { value: '', legacyValues: [] };
  }

  const code = optionCodeFromTypedValue(options, String(typedAnswer));
  return code
    ? { value: code, legacyValues: [] }
    : { value: '', legacyValues: [String(typedAnswer)] };
}

function initialMultiChoice(question, currentAnswer) {
  const options = question.options || [];
  const selectedOptions = Array.isArray(currentAnswer?.selectedOptions)
    ? currentAnswer.selectedOptions
    : [];
  const selectedCodes = [];
  const legacyValues = [];

  selectedOptions.forEach((selection) => {
    const code = optionCodeFromSelection(options, selection);
    if (code && !selectedCodes.includes(code)) {
      selectedCodes.push(code);
    } else if (!code) {
      const legacyValue = legacySelectionValue(selection);
      if (legacyValue && !legacyValues.includes(legacyValue)) legacyValues.push(legacyValue);
    }
  });

  choiceTokens(currentAnswer?.typedAnswer).forEach((typedValue) => {
    const code = optionCodeFromTypedValue(options, typedValue);
    if (code && !selectedCodes.includes(code)) {
      selectedCodes.push(code);
    } else if (!code && !legacyValues.includes(typedValue)) {
      legacyValues.push(typedValue);
    }
  });

  return { value: selectedCodes, legacyValues };
}

function initialAnswer(question) {
  const currentAnswer = question.currentAnswer;
  if (!currentAnswer) {
    return { value: emptyAnswer(question.questionType), legacyValues: [] };
  }

  if (question.questionType === QUESTION_TYPE.SINGLE_CHOICE) {
    return initialSingleChoice(question, currentAnswer);
  }
  if (question.questionType === QUESTION_TYPE.MULTI_CHOICE) {
    return initialMultiChoice(question, currentAnswer);
  }

  const typedAnswer = currentAnswer.typedAnswer;
  if (typedAnswer === undefined || typedAnswer === null) {
    return { value: emptyAnswer(question.questionType), legacyValues: [] };
  }
  if (question.questionType === QUESTION_TYPE.BOOLEAN && !['true', 'false'].includes(typedAnswer)) {
    return { value: '', legacyValues: [String(typedAnswer)] };
  }

  return { value: String(typedAnswer), legacyValues: [] };
}

export function createCorrectionAnswerState(questions = []) {
  return questions.reduce((state, question) => {
    const { value, legacyValues } = initialAnswer(question);
    state.answers[question.id] = value;
    if (legacyValues.length > 0) state.legacyAnswers[question.id] = legacyValues;
    return state;
  }, { answers: {}, legacyAnswers: {} });
}
