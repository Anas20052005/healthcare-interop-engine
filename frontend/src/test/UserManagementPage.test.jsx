import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import UserManagementPage from '../pages/UserManagementPage';
import userService from '../services/userService';
import hospitalService from '../services/hospitalService';
import { useAuth } from '../context/useAuth';

vi.mock('../services/userService');
vi.mock('../services/hospitalService');
vi.mock('../context/useAuth');

describe('UserManagementPage', () => {
  const mockUsers = [
    {
      id: 1,
      username: 'admin_sys',
      role: 'ADMIN',
      enabled: true,
      hospitalId: null,
      hospitalName: null,
      hospitalCode: null,
      createdAt: '2026-01-10T10:00:00Z',
    },
    {
      id: 2,
      username: 'dr_smith',
      role: 'USER',
      enabled: true,
      hospitalId: 1,
      hospitalName: 'St. Jude Children Hospital',
      hospitalCode: 'ST_JUDE',
      createdAt: '2026-01-15T12:30:00Z',
    },
    {
      id: 3,
      username: 'nurse_jones',
      role: 'USER',
      enabled: true,
      hospitalId: null, // Null hospital association!
      hospitalName: null,
      hospitalCode: null,
      createdAt: '2026-02-01T09:00:00Z',
    },
  ];

  const mockHospitals = [
    {
      id: 1,
      name: 'St. Jude Children Hospital',
      code: 'ST_JUDE',
      enabled: true,
    },
    {
      id: 2,
      name: 'Mercy General Hospital',
      code: 'MERCY_GEN',
      enabled: true,
    },
    {
      id: 3,
      name: 'Closed Care Clinic',
      code: 'CLOSED_01',
      enabled: false,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      user: { username: 'superadmin', role: 'ADMIN', hospitalId: null },
    });
    userService.getUsers.mockResolvedValue(mockUsers);
    hospitalService.getHospitals.mockResolvedValue(mockHospitals);
  });

  it('renders user list and KPI statistics correctly', async () => {
    render(
      <MemoryRouter>
        <UserManagementPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/User Account Administration/i)).toBeTruthy();

    await waitFor(() => {
      expect(userService.getUsers).toHaveBeenCalledTimes(1);
      expect(hospitalService.getHospitals).toHaveBeenCalledTimes(1);
      expect(screen.getByText('admin_sys')).toBeTruthy();
      expect(screen.getByText('dr_smith')).toBeTruthy();
      expect(screen.getByText('nurse_jones')).toBeTruthy();
      expect(screen.getByText('St. Jude Children Hospital')).toBeTruthy();
      // Unassigned hospital badge for nurse_jones
      expect(screen.getAllByText(/Unassigned \(No Hospital\)/i).length).toBeGreaterThan(0);
    });
  });

  it('opens edit modal and displays read-only username', async () => {
    render(
      <MemoryRouter>
        <UserManagementPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('nurse_jones')).toBeTruthy();
    });

    // Find Edit button for nurse_jones (3rd row)
    const editButtons = screen.getAllByRole('button', { name: /Edit/i });
    fireEvent.click(editButtons[2]);

    await waitFor(() => {
      expect(screen.getByText(/Editing settings for user/i)).toBeTruthy();
      const usernameInput = screen.getByLabelText(/Username/i);
      expect(usernameInput.value).toBe('nurse_jones');
      expect(usernameInput.hasAttribute('readonly')).toBe(true);
    });
  });

  it('allows assigning an active hospital to an existing clinical user with null hospital', async () => {
    userService.updateUser.mockResolvedValue({
      id: 3,
      username: 'nurse_jones',
      role: 'USER',
      enabled: true,
      hospitalId: 2,
      hospitalName: 'Mercy General Hospital',
      hospitalCode: 'MERCY_GEN',
    });

    render(
      <MemoryRouter>
        <UserManagementPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('nurse_jones')).toBeTruthy();
    });

    const editButtons = screen.getAllByRole('button', { name: /Edit/i });
    fireEvent.click(editButtons[2]);

    await waitFor(() => {
      expect(screen.getByText(/Hospital Association Required/i)).toBeTruthy();
    });

    // Select active hospital 'Mercy General Hospital' (value 2)
    const hospitalSelect = screen.getByLabelText(/Assigned Hospital Tenant/i);
    fireEvent.change(hospitalSelect, { target: { value: '2' } });
    expect(hospitalSelect.value).toBe('2');

    // Click Save Changes
    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(userService.updateUser).toHaveBeenCalledWith(3, {
        role: 'USER',
        hospitalId: 2,
        enabled: true,
      });
      expect(
        screen.getByText(/User "nurse_jones" updated successfully/i)
      ).toBeTruthy();
    });
  });

  it('displays error message when updateUser API fails', async () => {
    userService.updateUser.mockRejectedValueOnce(
      new Error('Cannot assign user to disabled hospital: Closed Care Clinic')
    );

    render(
      <MemoryRouter>
        <UserManagementPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('dr_smith')).toBeTruthy();
    });

    const editButtons = screen.getAllByRole('button', { name: /Edit/i });
    fireEvent.click(editButtons[1]);

    await waitFor(() => {
      expect(screen.getByText(/Edit User Account/i)).toBeTruthy();
    });

    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText(/Cannot assign user to disabled hospital/i)).toBeTruthy();
    });
  });
});
