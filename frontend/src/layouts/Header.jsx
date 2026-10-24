import React from 'react';
import { useLocation, Link } from 'react-router-dom';
import './Header.css';

export default function Header({ onToggleSidebar }) {
  const location = useLocation();

  const getPageTitle = (pathname) => {
    switch (pathname) {
      case '/dashboard':
        return { title: 'Dashboard', subtitle: 'Healthcare Interoperability Overview' };
      case '/convert':
        return { title: 'HL7 v2 → FHIR R4 Converter', subtitle: 'Transform clinical messages to FHIR resources' };
      case '/history':
        return { title: 'Conversion History', subtitle: 'Audit log of past conversions' };
      case '/destinations':
        return { title: 'FHIR Destinations', subtitle: 'Configure remote endpoints and inspect delivery channels' };
      case '/audit-logs':
        return { title: 'System Audit Logs', subtitle: 'Compliance trails, access events, and delivery records' };
      case '/patients':
        return { title: 'Clinical Records', subtitle: 'Browse hospital patients, encounters, and lab observations' };
      case '/users':
      case '/users/create':
        return { title: 'User Administration', subtitle: 'Manage healthcare staff and administrator accounts' };
      case '/hospitals':
        return { title: 'Hospital Management', subtitle: 'Manage healthcare institutions and tenant configuration' };
      default:
        return { title: 'Interoperability Engine', subtitle: 'Clinical Data Gateway' };
    }
  };

  const { title, subtitle } = getPageTitle(location.pathname);

  return (
    <header className="app-header">
      <div className="header-left">
        <button
          type="button"
          className="sidebar-toggle-btn"
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <div className="header-titles">
          <h1 className="header-page-title">{title}</h1>
          <p className="header-page-subtitle">{subtitle}</p>
        </div>
      </div>

      <div className="header-right">
        <div className="header-status-pill">
          <span className="status-dot-pulse" />
          <span className="status-text">Engine Operational</span>
        </div>

        <span className="header-fhir-badge">
          FHIR R4
        </span>

        {location.pathname !== '/convert' && (
          <Link to="/convert" className="header-cta-btn">
            <svg viewBox="0 0 16 16" fill="currentColor">
              <path d="M8 2a.75.75 0 01.75.75v4.5h4.5a.75.75 0 010 1.5h-4.5v4.5a.75.75 0 01-1.5 0v-4.5h-4.5a.75.75 0 010-1.5h4.5v-4.5A.75.75 0 018 2z" />
            </svg>
            <span>Convert HL7</span>
          </Link>
        )}
      </div>
    </header>
  );
}
