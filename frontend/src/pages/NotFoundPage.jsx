import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';
import './NotFoundPage.css';

export default function NotFoundPage() {
  return (
    <div className="not-found-page">
      <div className="not-found-card">
        <div className="not-found-code">404</div>
        <h2 className="not-found-title">Page Not Found</h2>
        <p className="not-found-desc">
          The requested route does not exist in the Healthcare Interoperability Engine console.
        </p>
        <Link to="/dashboard">
          <Button variant="primary" size="md">
            Return to Dashboard
          </Button>
        </Link>
      </div>
    </div>
  );
}
