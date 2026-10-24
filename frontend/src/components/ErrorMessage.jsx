import React from 'react';
import './ErrorMessage.css';

/**
 * Reusable ErrorMessage Alert Component
 */
export default function ErrorMessage({
  title = 'Conversion Error',
  message,
  onDismiss,
  className = '',
}) {
  if (!message) return null;

  return (
    <div className={`error-alert ${className}`} role="alert">
      <div className="error-alert-icon">
        <svg viewBox="0 0 20 20" fill="currentColor">
          <path
            fillRule="evenodd"
            d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z"
            clipRule="evenodd"
          />
        </svg>
      </div>
      <div className="error-alert-content">
        {title && <h5 className="error-alert-title">{title}</h5>}
        <p className="error-alert-text">{message}</p>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="error-alert-dismiss"
          aria-label="Dismiss error"
        >
          <svg viewBox="0 0 16 16" fill="currentColor">
            <path d="M3.72 3.72a.75.75 0 011.06 0L8 6.94l3.22-3.22a.75.75 0 111.06 1.06L9.06 8l3.22 3.22a.75.75 0 11-1.06 1.06L8 9.06l-3.22 3.22a.75.75 0 01-1.06-1.06L6.94 8 3.72 4.78a.75.75 0 010-1.06z" />
          </svg>
        </button>
      )}
    </div>
  );
}
