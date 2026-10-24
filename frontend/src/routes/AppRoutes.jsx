import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AppLayout from '../layouts/AppLayout';
import LoginPage from '../pages/LoginPage';
import DashboardPage from '../pages/DashboardPage';
import ConvertPage from '../pages/ConvertPage';
import HistoryPage from '../pages/HistoryPage';
import CreateUserPage from '../pages/CreateUserPage';
import UserManagementPage from '../pages/UserManagementPage';
import HospitalPage from '../pages/HospitalPage';
import DestinationsPage from '../pages/DestinationsPage';
import AuditLogsPage from '../pages/AuditLogsPage';
import ClinicalRecordsPage from '../pages/ClinicalRecordsPage';
import NotFoundPage from '../pages/NotFoundPage';

import ProtectedRoute from '../components/ProtectedRoute';

/**
 * Main Application Routing
 * Enforces authentication guards on private routes and directs unauthenticated users to /login
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* Public Login Route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Authenticated Layout Routes guarded by ProtectedRoute */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="convert" element={<ConvertPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="destinations" element={<DestinationsPage />} />
        <Route path="audit-logs" element={<AuditLogsPage />} />
        <Route path="patients" element={<ClinicalRecordsPage />} />
        <Route
          path="hospitals"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <HospitalPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="users"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <UserManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="users/create"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <CreateUserPage />
            </ProtectedRoute>
          }
        />
        <Route path="admin/users" element={<Navigate to="/users" replace />} />
        <Route path="admin/users/create" element={<Navigate to="/users/create" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
