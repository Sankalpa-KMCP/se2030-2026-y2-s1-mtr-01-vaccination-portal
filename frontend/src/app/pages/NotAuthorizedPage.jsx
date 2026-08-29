import React from 'react';
import { Link } from 'react-router-dom';

export default function NotAuthorizedPage() {
  return (
    <section className="row justify-content-center py-5">
      <div className="col-12 col-md-8 col-lg-6">
        <div className="card border-0 shadow-sm">
          <div className="card-body p-4 p-md-5 text-center">
            <div className="display-6 text-warning mb-3" aria-hidden="true">403</div>
            <h1 className="h3 text-dark mb-3">Not Authorized</h1>
            <p className="text-muted mb-4">
              Your authenticated account does not have permission to access this page.
            </p>
            <Link className="btn btn-primary" to="/app/overview">
              Return to Overview
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
