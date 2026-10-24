import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HistoryPage from '../pages/HistoryPage';
import historyService from '../services/historyService';
import destinationService from '../services/destinationService';

vi.mock('../services/historyService');
vi.mock('../services/destinationService');

describe('HistoryPage', () => {
  const mockRecords = [
    {
      conversionId: 101,
      messageControlId: 'MSG001',
      messageType: 'ADT^A01',
      status: 'SUCCESS',
      patientIdentifier: 'PAT-12345',
      patientId: 'PAT-12345',
      encounterId: 'ENC-999',
      observationCount: 3,
      createdAt: '2026-10-16T12:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    historyService.getHistory.mockResolvedValue(mockRecords);
    destinationService.getDestinations.mockResolvedValue([]);
    destinationService.getConversionDeliveries.mockResolvedValue([]);
  });

  it('renders without runtime errors and loads conversion history', async () => {
    render(
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>
    );

    // Verify page header renders
    expect(screen.getByText(/Conversion Audit History/i)).toBeTruthy();

    // Verify loadHistory fetched and rendered records
    await waitFor(() => {
      expect(historyService.getHistory).toHaveBeenCalledTimes(1);
      expect(screen.getByText('MSG001')).toBeTruthy();
      expect(screen.getByText('PAT-12345')).toBeTruthy();
    });
  });

  it('allows reloading conversion records when Refresh button is clicked', async () => {
    render(
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('MSG001')).toBeTruthy();
    });

    const refreshBtn = screen.getByRole('button', { name: /Refresh/i });
    expect(refreshBtn).toBeTruthy();

    fireEvent.click(refreshBtn);

    await waitFor(() => {
      expect(historyService.getHistory).toHaveBeenCalledTimes(2);
    });
  });

  it('handles error state gracefully when history loading fails', async () => {
    historyService.getHistory.mockRejectedValueOnce(new Error('Network error loading history'));

    render(
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Audit History Error/i)).toBeTruthy();
      expect(screen.getByText(/Network error loading history/i)).toBeTruthy();
    });
  });
});
