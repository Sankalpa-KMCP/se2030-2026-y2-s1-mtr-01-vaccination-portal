import { beforeEach, describe, expect, it, vi } from 'vitest';

const clientMocks = vi.hoisted(() => ({
  apiRequest: vi.fn(),
  parseApiResponse: vi.fn(),
}));

vi.mock('../../../services/apiClient', () => clientMocks);

import {
  createAnswerOption,
  createQuestion,
  deleteAnswerOption,
  deleteQuestion,
  getAnswerOption,
  getQuestion,
  listAnswerOptions,
  listQuestions,
  updateAnswerOption,
  updateQuestion,
} from './screeningApi';

describe('screeningApi', () => {
  const rawResponse = { status: 200 };

  beforeEach(() => {
    vi.clearAllMocks();
    clientMocks.apiRequest.mockResolvedValue(rawResponse);
    clientMocks.parseApiResponse.mockResolvedValue({ result: 'parsed' });
  });

  it('lists all questions through the exact backend path', async () => {
    await expect(listQuestions()).resolves.toEqual({ result: 'parsed' });

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions', { method: 'GET' });
    expect(clientMocks.parseApiResponse).toHaveBeenCalledWith(rawResponse);
  });

  it('supports the backend activeOnly question filter', async () => {
    await listQuestions({ activeOnly: true });

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions?activeOnly=true', { method: 'GET' });
  });

  it('gets a question by ID', async () => {
    await getQuestion(12);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12', { method: 'GET' });
  });

  it('creates a question with the backend DTO body', async () => {
    const payload = {
      code: 'TRAVEL_HISTORY',
      text: 'Have you travelled recently?',
      description: null,
      questionType: 'BOOLEAN',
      defaultRequired: true,
    };

    await createQuestion(payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('updates a question through PUT using only mutable fields', async () => {
    const payload = {
      text: 'Updated question?',
      description: 'Updated description',
      defaultRequired: false,
      active: true,
    };

    await updateQuestion(12, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12', {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  });

  it('deletes a question through the exact backend path', async () => {
    await deleteQuestion(12);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12', { method: 'DELETE' });
  });

  it('lists and gets answer options through their backend paths', async () => {
    await listAnswerOptions(12);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/questions/12/options', { method: 'GET' });

    await getAnswerOption(44);
    expect(clientMocks.apiRequest).toHaveBeenLastCalledWith('/v1/screening/options/44', { method: 'GET' });
  });

  it('creates an answer option with the exact DTO body', async () => {
    const payload = { code: 'YES', label: 'Yes', typedValue: 'true', displayOrder: 1 };

    await createAnswerOption(12, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/questions/12/options', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  });

  it('updates an answer option through PUT', async () => {
    const payload = { code: 'NO', label: 'No', typedValue: 'false', displayOrder: 2 };

    await updateAnswerOption(44, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/options/44', {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  });

  it('deletes an answer option through the exact backend path', async () => {
    await deleteAnswerOption(44);

    expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/screening/options/44', { method: 'DELETE' });
  });
});
