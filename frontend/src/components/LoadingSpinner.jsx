import React from 'react';
import './LoadingSpinner.css';

/**
 * Reusable Loading Spinner Component
 */
export default function LoadingSpinner({
  size = 'md',
  message = 'Loading...',
  inline = false,
  className = '',
}) {
  return (
    <div
      className={`spinner-container ${inline ? 'spinner-inline' : 'spinner-block'} spinner-${size} ${className}`}
      role="status"
      aria-live="polite"
    >
      <svg className="spinner-svg" viewBox="0 0 24 24" fill="none">
        <circle
          className="spinner-track"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="3"
        />
        <path
          className="spinner-head"
          fill="currentColor"
          d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z"
        />
      </svg>
      {message && <span className="spinner-text">{message}</span>}
      <span className="sr-only">Loading</span>
    </div>
  );
}
