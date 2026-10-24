import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HospitalPage from '../pages/HospitalPage';
import hospitalService from '../services/hospitalService';

vi.mock('../services/hospitalService');

describe('HospitalPage', () => {
  const mockHospitals = [
    {
      id: 1,
      name: 'St. Jude Children Hospital',
      code: 'ST_JUDE',
      email: 'contact@stjude.org',
      phone: '555-0199',
      address: '262 Danny Thomas Pl, Memphis, TN',
      enabled: true,
      createdAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 2,
      name: 'Mercy General Hospital',
      code: 'MERCY_GEN',
      email: 'info@mercy.org',
      phone: '555-0122',
      address: '123 Mercy Ave, Springfield',
      enabled: false,
      createdAt: '2026-02-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    hospitalService.getHospitals.mockResolvedValue(mockHospitals);
  });

  it('renders without runtime errors and loads hospital data', async () => {
    render(
      <MemoryRouter>
        <HospitalPage />
      </MemoryRouter>
    );

    // Verify page header renders
    expect(screen.getByText(/Hospital Tenant Management/i)).toBeTruthy();

    // Verify loadHospitals fetched and rendered records
    await waitFor(() => {
      expect(hospitalService.getHospitals).toHaveBeenCalledTimes(1);
      expect(screen.getByText('St. Jude Children Hospital')).toBeTruthy();
      expect(screen.getByText('ST_JUDE')).toBeTruthy();
      expect(screen.getByText('Mercy General Hospital')).toBeTruthy();
    });
  });

  it('allows reloading hospital records when Refresh button is clicked', async () => {
    render(
      <MemoryRouter>
        <HospitalPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('St. Jude Children Hospital')).toBeTruthy();
    });

    const refreshBtn = screen.getByRole('button', { name: /Refresh/i });
    expect(refreshBtn).toBeTruthy();

    fireEvent.click(refreshBtn);

    await waitFor(() => {
      expect(hospitalService.getHospitals).toHaveBeenCalledTimes(2);
    });
  });

  it('handles error state gracefully when hospital loading fails', async () => {
    hospitalService.getHospitals.mockRejectedValueOnce(new Error('Network error loading hospitals'));

    render(
      <MemoryRouter>
        <HospitalPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Network error loading hospitals/i)).toBeTruthy();
    });
  });
});
