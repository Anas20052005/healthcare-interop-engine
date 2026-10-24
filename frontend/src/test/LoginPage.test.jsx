import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import * as useAuthModule from '../context/useAuth';

describe('LoginPage', () => {
  const mockLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      login: mockLogin,
      user: null,
      isAuthenticated: false,
    });
  });

  it('renders login form with empty initial inputs (FIND-06 verified)', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const usernameInput = screen.getByLabelText(/Username/i);
    const passwordInput = screen.getByLabelText(/^Password/i);

    expect(usernameInput.value).toBe('');
    expect(passwordInput.value).toBe('');
  });

  it('validates empty credentials on submission', async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const form = screen.getByRole('button', { name: /Sign In/i }).closest('form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText(/Please enter both username and password/i)).toBeTruthy();
    });
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('submits credentials when valid and handles login error', async () => {
    mockLogin.mockRejectedValueOnce({ userMessage: 'Invalid credentials provided' });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const usernameInput = screen.getByLabelText(/Username/i);
    const passwordInput = screen.getByLabelText(/^Password/i);
    const submitBtn = screen.getByRole('button', { name: /Sign In/i });

    fireEvent.change(usernameInput, { target: { value: 'doctor_smith' } });
    fireEvent.change(passwordInput, { target: { value: 'Secret123!' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('doctor_smith', 'Secret123!');
      expect(screen.getByText(/Invalid credentials provided/i)).toBeTruthy();
    });
  });

  it('toggles password visibility when button is clicked', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const passwordInput = screen.getByLabelText(/^Password/i);
    expect(passwordInput.type).toBe('password');

    const toggleBtn = screen.getByRole('button', { name: /Show password/i });
    fireEvent.click(toggleBtn);

    expect(passwordInput.type).toBe('text');
  });

  it('never displays development demo banner or default credentials text', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    expect(screen.queryByText(/Development Demo/i)).toBeNull();
    expect(screen.queryByText(/Default credentials/i)).toBeNull();
    expect(screen.queryByText(/admin123/i)).toBeNull();
  });
});
