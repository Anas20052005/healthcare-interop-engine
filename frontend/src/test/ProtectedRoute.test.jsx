import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import * as useAuthModule from '../context/useAuth';

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('redirects unauthenticated users to /login', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      isAuthenticated: false,
      user: null,
    });

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/login" element={<div>Login Page Target</div>} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <div>Protected Dashboard Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login Page Target')).toBeTruthy();
    expect(screen.queryByText('Protected Dashboard Content')).toBeNull();
  });

  it('renders children for authenticated users with valid role', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      user: { username: 'dr_who', role: 'ADMIN' },
    });

    render(
      <MemoryRouter initialEntries={['/hospitals']}>
        <Routes>
          <Route
            path="/hospitals"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <div>Admin Hospital Management</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Admin Hospital Management')).toBeTruthy();
  });

  it('redirects authenticated non-admin users away from admin-only routes', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      user: { username: 'nurse_joy', role: 'USER' },
    });

    render(
      <MemoryRouter initialEntries={['/hospitals']}>
        <Routes>
          <Route path="/dashboard" element={<div>Dashboard Fallback</div>} />
          <Route
            path="/hospitals"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <div>Admin Only View</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Dashboard Fallback')).toBeTruthy();
    expect(screen.queryByText('Admin Only View')).toBeNull();
  });
});
