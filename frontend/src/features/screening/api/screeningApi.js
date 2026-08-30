import { apiRequest, parseApiResponse } from '../../../services/apiClient';

const SCREENING_BASE_PATH = '/v1/screening';

async function screeningRequest(endpoint, options = {}) {
  const response = await apiRequest(`${SCREENING_BASE_PATH}${endpoint}`, options);
  return parseApiResponse(response);
}

export function listQuestions({ activeOnly = false } = {}) {
  const query = activeOnly ? '?activeOnly=true' : '';
  return screeningRequest(`/questions${query}`, { method: 'GET' });
}

export function getQuestion(questionId) {
  return screeningRequest(`/questions/${questionId}`, { method: 'GET' });
}

export function createQuestion(payload) {
  return screeningRequest('/questions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateQuestion(questionId, payload) {
  return screeningRequest(`/questions/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteQuestion(questionId) {
  return screeningRequest(`/questions/${questionId}`, { method: 'DELETE' });
}

export function listAnswerOptions(questionId) {
  return screeningRequest(`/questions/${questionId}/options`, { method: 'GET' });
}

export function getAnswerOption(optionId) {
  return screeningRequest(`/options/${optionId}`, { method: 'GET' });
}

export function createAnswerOption(questionId, payload) {
  return screeningRequest(`/questions/${questionId}/options`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateAnswerOption(optionId, payload) {
  return screeningRequest(`/options/${optionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteAnswerOption(optionId) {
  return screeningRequest(`/options/${optionId}`, { method: 'DELETE' });
}
