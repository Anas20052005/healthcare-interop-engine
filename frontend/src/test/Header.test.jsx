import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Header from '../layouts/Header';

describe('Header component', () => {
  it('renders correct title and subtitle for dashboard route', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Header onToggleSidebar={() => {}} />
      </MemoryRouter>
    );
    expect(screen.getByText('Dashboard')).toBeTruthy();
    expect(screen.getByText('Healthcare Interoperability Overview')).toBeTruthy();
  });

  it('renders correct title for /destinations route', () => {
    render(
      <MemoryRouter initialEntries={['/destinations']}>
        <Header onToggleSidebar={() => {}} />
      </MemoryRouter>
    );
    expect(screen.getByText('FHIR Destinations')).toBeTruthy();
  });

  it('renders correct title for /audit-logs route', () => {
    render(
      <MemoryRouter initialEntries={['/audit-logs']}>
        <Header onToggleSidebar={() => {}} />
      </MemoryRouter>
    );
    expect(screen.getByText('System Audit Logs')).toBeTruthy();
  });

  it('renders correct title for /convert route', () => {
    render(
      <MemoryRouter initialEntries={['/convert']}>
        <Header onToggleSidebar={() => {}} />
      </MemoryRouter>
    );
    expect(screen.getByText('HL7 v2 → FHIR R4 Converter')).toBeTruthy();
  });
});
