import React, { useEffect, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import {
  createQuestion,
  deleteQuestion,
  listQuestions,
  updateQuestion,
} from '../api/screeningApi';
import AnswerOptionsModal from '../components/AnswerOptionsModal';
import DeleteQuestionModal from '../components/DeleteQuestionModal';
import QuestionFormModal from '../components/QuestionFormModal';
import {
  QUESTION_TYPE_LABELS,
  isChoiceQuestionType,
} from '../constants/screeningConstants';

export default function ScreeningQuestionsPage() {
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [formQuestion, setFormQuestion] = useState(undefined);
  const [formError, setFormError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [optionsQuestion, setOptionsQuestion] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    let active = true;
    listQuestions()
      .then((data) => {
        if (active) setQuestions(Array.isArray(data) ? data : []);
      })
      .catch((error) => {
        if (active) setLoadError(error);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const openCreate = () => {
    setFormQuestion(null);
    setFormError(null);
    setSuccessMessage(null);
  };

  const openEdit = (question) => {
    setFormQuestion(question);
    setFormError(null);
    setSuccessMessage(null);
  };

  const closeForm = () => {
    if (!saving) setFormQuestion(undefined);
  };

  const handleSave = async (payload) => {
    const editing = Boolean(formQuestion);
    setSaving(true);
    setFormError(null);
    try {
      const saved = editing
        ? await updateQuestion(formQuestion.id, payload)
        : await createQuestion(payload);
      setQuestions((current) => editing
        ? current.map((question) => question.id === saved.id ? saved : question)
        : [...current, saved]);
      setFormQuestion(undefined);
      setSuccessMessage(editing ? 'Screening question updated.' : 'Screening question created.');
    } catch (error) {
      setFormError(error);
    } finally {
      setSaving(false);
    }
  };

  const openDelete = (question) => {
    setDeleteTarget(question);
    setDeleteError(null);
    setSuccessMessage(null);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteQuestion(deleteTarget.id);
      setQuestions((current) => current.filter((question) => question.id !== deleteTarget.id));
      setDeleteTarget(null);
      setSuccessMessage('Screening question deleted.');
    } catch (error) {
      setDeleteError(error);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <section aria-labelledby="screeningQuestionsTitle">
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-start gap-3 mb-4">
        <div>
          <h1 className="h3 mb-1" id="screeningQuestionsTitle">Screening Questions</h1>
          <p className="text-muted mb-0">
            Configure the reusable question catalogue and answer options used by screening questionnaires.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          Create Question
        </button>
      </div>

      {successMessage && (
        <div className="alert alert-success alert-dismissible shadow-sm" role="status">
          {successMessage}
          <button type="button" className="btn-close" aria-label="Dismiss success" onClick={() => setSuccessMessage(null)} />
        </div>
      )}

      <ApiErrorAlert error={loadError} onDismiss={() => setLoadError(null)} />

      {loading ? (
        <LoadingSpinner message="Loading screening questions..." />
      ) : questions.length === 0 ? (
        <div className="card border-0 shadow-sm">
          <div className="card-body text-center py-5">
            <h2 className="h5">No screening questions have been configured yet.</h2>
            <p className="text-muted mb-3">Create the first catalogue question to begin configuring MF-01 screening.</p>
            <button type="button" className="btn btn-primary" onClick={openCreate}>Create First Question</button>
          </div>
        </div>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="card-header bg-white py-3">
            <div className="d-flex justify-content-between align-items-center">
              <h2 className="h5 mb-0">Question Catalogue</h2>
              <span className="badge text-bg-light border">{questions.length} question{questions.length === 1 ? '' : 's'}</span>
            </div>
          </div>
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">Code</th>
                  <th scope="col">Question</th>
                  <th scope="col">Type</th>
                  <th scope="col">Required</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {questions.map((question) => (
                  <tr key={question.id}>
                    <td className="font-monospace fw-semibold">{question.code}</td>
                    <td>
                      <div className="fw-semibold">{question.text}</div>
                      {question.description && <div className="small text-muted mt-1">{question.description}</div>}
                    </td>
                    <td><span className="badge text-bg-light border">{QUESTION_TYPE_LABELS[question.questionType] || question.questionType}</span></td>
                    <td>{question.defaultRequired ? <span className="badge text-bg-info">Required</span> : <span className="text-muted">Optional</span>}</td>
                    <td>{question.active ? <span className="badge text-bg-success">Active</span> : <span className="badge text-bg-secondary">Inactive</span>}</td>
                    <td className="text-end">
                      <div className="d-inline-flex flex-wrap justify-content-end gap-1">
                        {isChoiceQuestionType(question.questionType) && (
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            aria-label={`Manage options for ${question.code}`}
                            onClick={() => setOptionsQuestion(question)}
                          >
                            Options
                          </button>
                        )}
                        <button type="button" className="btn btn-outline-primary btn-sm" aria-label={`Edit ${question.code}`} onClick={() => openEdit(question)}>Edit</button>
                        <button type="button" className="btn btn-outline-danger btn-sm" aria-label={`Delete ${question.code}`} onClick={() => openDelete(question)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <QuestionFormModal
        show={formQuestion !== undefined}
        question={formQuestion || null}
        processing={saving}
        apiError={formError}
        onClose={closeForm}
        onSubmit={handleSave}
      />
      <DeleteQuestionModal
        question={deleteTarget}
        processing={deleting}
        error={deleteError}
        onClose={() => !deleting && setDeleteTarget(null)}
        onConfirm={handleDelete}
      />
      <AnswerOptionsModal question={optionsQuestion} onClose={() => setOptionsQuestion(null)} />
    </section>
  );
}
