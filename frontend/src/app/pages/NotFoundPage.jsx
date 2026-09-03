import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/context/AuthContext';

export default function NotFoundPage() {
  const { isAuthenticated } = useAuth();
  const returnPath = isAuthenticated ? '/app/overview' : '/';

  return (
    <section className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 p-md-5 text-center">
              <div className="display-6 text-secondary mb-3" aria-hidden="true">404</div>
              <h1 className="h3 text-dark mb-3">Page Not Found</h1>
              <p className="text-muted mb-4">
                The page you requested does not exist or has been moved.
              </p>
              <Link className="btn btn-primary" to={returnPath}>
                {isAuthenticated ? 'Return to Overview' : 'Return to Portal Home'}
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
