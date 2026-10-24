import React from 'react';
import './StatusBadge.css';

/**
 * Reusable StatusBadge component for healthcare integration status
 */
export default function StatusBadge({ status, className = '' }) {
  if (!status) return null;

  const normalized = status.toUpperCase();
  let badgeClass = 'badge-secondary';
  let label = status;
  let icon = null;

  switch (normalized) {
    case 'SUCCESS':
    case 'OPERATIONAL':
    case 'COMPLETED':
    case 'FINAL':
    case 'UP':
      badgeClass = 'badge-success';
      icon = (
        <svg viewBox="0 0 16 16" fill="currentColor" className="badge-icon">
          <path d="M13.78 4.22a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0L2.22 9.28a.75.75 0 011.06-1.06L6 10.94l6.72-6.72a.75.75 0 011.06 0z" />
        </svg>
      );
      break;

    case 'FAILED':
    case 'FAILURE':
    case 'ERROR':
    case 'DOWN':
      badgeClass = 'badge-error';
      icon = (
        <svg viewBox="0 0 16 16" fill="currentColor" className="badge-icon">
          <path d="M3.72 3.72a.75.75 0 011.06 0L8 6.94l3.22-3.22a.75.75 0 111.06 1.06L9.06 8l3.22 3.22a.75.75 0 11-1.06 1.06L8 9.06l-3.22 3.22a.75.75 0 01-1.06-1.06L6.94 8 3.72 4.78a.75.75 0 010-1.06z" />
        </svg>
      );
      break;

    case 'WARNING':
    case 'PENDING':
    case 'IN-PROGRESS':
    case 'IN_PROGRESS':
    case 'RETRYING':
    case 'DEGRADED':
      badgeClass = 'badge-warning';
      icon = (
        <svg viewBox="0 0 16 16" fill="currentColor" className="badge-icon">
          <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0114.082 15H1.918a1.75 1.75 0 01-1.543-2.575L6.457 1.047zM8 5a.75.75 0 00-.75.75v3.5a.75.75 0 001.5 0v-3.5A.75.75 0 008 5zm0 8a1 1 0 100-2 1 1 0 000 2z" />
        </svg>
      );
      break;

    default:
      badgeClass = 'badge-info';
      break;
  }

  return (
    <span className={`status-badge ${badgeClass} ${className}`}>
      {icon}
      <span>{label}</span>
    </span>
  );
}
