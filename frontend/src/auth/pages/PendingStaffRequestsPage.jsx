import React, { useCallback, useEffect, useState } from 'react';
import {
  approveStaffRequest,
  getStaffRequest,
  listStaffRequests,
  rejectStaffRequest,
} from '../api/adminStaffRequestsApi';
import ApiErrorAlert from '../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import EmptyState from '../../components/common/EmptyState';
import SuccessAlert from '../../components/common/SuccessAlert';
import { staffRequestStatusVariants } from '../../components/common/statusVariants';
import StaffRequestReviewModal from '../components/StaffRequestReviewModal';
import { getRoleLabel } from '../constants/authRoles';

function formatDate(value) {
  if (!value) return 'Not available';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export default function PendingStaffRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [modal, setModal] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const loadRequests = useCallback(async () => {
    setError(null);
    try {
      const data = await listStaffRequests();
      setRequests(Array.isArray(data) ? data : []);
    } catch (loadError) {
      setError(loadError);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const openView = async (request) => {
    setError(null);
    try {
      const detail = await getStaffRequest(request.id);
      setModal({ mode: 'view', request: detail });
    } catch (detailError) {
      setError(detailError);
    }
  };

  const performAction = async (action, success) => {
    setError(null);
    setSuccessMessage('');
    setIsProcessing(true);
    try {
      await action();
      setModal(null);
      setSuccessMessage(success);
      await loadRequests();
    } catch (actionError) {
      setError(actionError);
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading pending staff account requests..." />;
  }

  return (
    <section aria-labelledby="staff-requests-title">
      <PageHeader
        id="staff-requests-title"
        title="Staff Account Requests"
        eyebrow="Administrator Only"
        eyebrowClass="badge bg-danger-subtle text-danger-emphasis border border-danger-subtle fw-semibold"
        subtitle="Review pending requests and assign only the authoritative role you approve."
        actions={(
          <button type="button" className="btn btn-outline-primary btn-sm" onClick={loadRequests}>Refresh</button>
        )}
      />

      <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
      {successMessage && (
        <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage('')} />
      )}

      {requests.length === 0 ? (
        <div className="card border-0 shadow-sm">
          <EmptyState icon="bi-person-badge" title="No pending requests">
            There are currently no staff account requests waiting for review.
          </EmptyState>
        </div>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">Applicant</th>
                  <th scope="col">Contact</th>
                  <th scope="col">Requested Role</th>
                  <th scope="col">Submitted</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((request) => (
                  <tr key={request.id}>
                    <td><strong>{request.fullName}</strong><div className="small text-muted">{request.email}</div></td>
                    <td>{request.phoneNumber}</td>
                    <td>{getRoleLabel(request.requestedRoleCode)}</td>
                    <td>{formatDate(request.createdAt)}</td>
                    <td><StatusBadge status={request.accountStatus} variants={staffRequestStatusVariants} /></td>
                    <td>
                      <div className="d-flex justify-content-end gap-2">
                        <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => openView(request)}>View</button>
                        <button type="button" className="btn btn-sm btn-outline-success" onClick={() => setModal({ mode: 'approve', request })}>Approve</button>
                        <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => setModal({ mode: 'reject', request })}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {modal && (
        <StaffRequestReviewModal
          mode={modal.mode}
          request={modal.request}
          isProcessing={isProcessing}
          onClose={() => setModal(null)}
          onApprove={(roleCode) => performAction(
            () => approveStaffRequest(modal.request.id, roleCode),
            `${modal.request.fullName}'s staff account was approved.`,
          )}
          onReject={(reason) => performAction(
            () => rejectStaffRequest(modal.request.id, reason),
            `${modal.request.fullName}'s staff account request was rejected.`,
          )}
        />
      )}
    </section>
  );
}
