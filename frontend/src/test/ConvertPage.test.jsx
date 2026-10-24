import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ConvertPage from '../pages/ConvertPage';
import destinationService from '../services/destinationService';
import conversionService from '../services/conversionService';
import { SAMPLE_HL7_MESSAGE } from '../services/mockData';

vi.mock('../services/destinationService');
vi.mock('../services/conversionService');

describe('ConvertPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    destinationService.getDestinations.mockResolvedValue([
      { id: 1, name: 'Local Mock FHIR Server', baseUrl: 'http://localhost:8080/api/mock-fhir', enabled: true, mock: true }
    ]);
  });

  it('renders with sample HL7 message and allows clearing input', async () => {
    const { container } = render(
      <MemoryRouter>
        <ConvertPage />
      </MemoryRouter>
    );

    const textarea = container.querySelector('#hl7Input');
    expect(textarea.value).toContain('MSH|^~\\&');

    const clearBtn = screen.getByRole('button', { name: /Clear/i });
    fireEvent.click(clearBtn);

    expect(textarea.value).toBe('');
  });

  it('allows loading example HL7 message', async () => {
    const { container } = render(
      <MemoryRouter>
        <ConvertPage />
      </MemoryRouter>
    );

    const textarea = container.querySelector('#hl7Input');
    const clearBtn = screen.getByRole('button', { name: /Clear/i });
    fireEvent.click(clearBtn);
    expect(textarea.value).toBe('');

    const loadExampleBtn = screen.getByRole('button', { name: /Load Example/i });
    fireEvent.click(loadExampleBtn);

    expect(textarea.value).toBe(SAMPLE_HL7_MESSAGE);
  });

  it('handles conversion errors properly', async () => {
    conversionService.convertHL7.mockRejectedValueOnce({
      userMessage: 'Unsupported HL7 message type: ORU^R01'
    });

    render(
      <MemoryRouter>
        <ConvertPage />
      </MemoryRouter>
    );

    const convertBtn = screen.getByRole('button', { name: /Convert to FHIR/i });
    fireEvent.click(convertBtn);

    await waitFor(() => {
      expect(screen.getByText(/Unsupported HL7 message type: ORU\^R01/i)).toBeTruthy();
    });
  });
});
