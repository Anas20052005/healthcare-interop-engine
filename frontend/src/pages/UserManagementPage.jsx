import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/Card';
import Input from '../components/Input';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import userService from '../services/userService';
import hospitalService from '../services/hospitalService';
import { useAuth } from '../context/useAuth';
import './UserManagementPage.css';

/**
 * User Management Page
 * Provides comprehensive user administration: lists existing accounts, filters by role/hospital/status,
 * and allows authorized administrators to edit user roles, hospital affiliations, and active states.
 */
export default function UserManagementPage() {
  const { user: currentUser } = useAuth();
  const isPlatformAdmin = currentUser?.role === 'ADMIN' && !currentUser?.hospitalId;

  const [users, setUsers] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [hospitalFilter, setHospitalFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Edit Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [editFormData, setEditFormData] = useState({
    role: 'USER',
    hospitalId: '',
    enabled: true,
    password: '',
  });
  const [editFormErrors, setEditFormErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);
  const [modalError, setModalError] = useState('');

  // View Details Modal State
  const [viewingUser, setViewingUser] = useState(null);

  // Load users and hospitals
  const loadData = useCallback(async () => {
    setIsLoading(true);
    setApiError('');
    try {
      const [usersData, hospitalsData] = await Promise.all([
        userService.getUsers(),
        hospitalService.getHospitals().catch((err) => {
          console.warn('Could not load hospitals list:', err);
          return [];
        }),
      ]);
      setUsers(Array.isArray(usersData) ? usersData : []);
      setHospitals(Array.isArray(hospitalsData) ? hospitalsData : []);
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to retrieve user accounts.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    async function init() {
      try {
        const [usersData, hospitalsData] = await Promise.all([
          userService.getUsers(),
          hospitalService.getHospitals().catch(() => []),
        ]);
        if (!ignore) {
          setUsers(Array.isArray(usersData) ? usersData : []);
          setHospitals(Array.isArray(hospitalsData) ? hospitalsData : []);
          setIsLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          setApiError(err.userMessage || err.message || 'Failed to retrieve user accounts.');
          setIsLoading(false);
        }
      }
    }
    init();
    return () => {
      ignore = true;
    };
  }, []);

  // Filtered Users List
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      // Role filter
      if (roleFilter !== 'ALL' && u.role !== roleFilter) {
        return false;
      }

      // Status filter
      if (statusFilter === 'ACTIVE' && !u.enabled) return false;
      if (statusFilter === 'INACTIVE' && u.enabled) return false;

      // Hospital filter
      if (hospitalFilter === 'UNASSIGNED') {
        if (u.hospitalId != null) return false;
      } else if (hospitalFilter !== 'ALL') {
        if (String(u.hospitalId) !== String(hospitalFilter)) return false;
      }

      // Search query
      const q = searchQuery.toLowerCase().trim();
      if (q) {
        const matchesUsername = u.username?.toLowerCase().includes(q);
        const matchesHospital = u.hospitalName?.toLowerCase().includes(q);
        const matchesCode = u.hospitalCode?.toLowerCase().includes(q);
        const matchesRole = u.role?.toLowerCase().includes(q);
        if (!matchesUsername && !matchesHospital && !matchesCode && !matchesRole) {
          return false;
        }
      }

      return true;
    });
  }, [users, searchQuery, roleFilter, hospitalFilter, statusFilter]);

  // Statistics
  const totalCount = users.length;
  const clinicalCount = users.filter((u) => u.role === 'USER').length;
  const adminCount = users.filter((u) => u.role === 'ADMIN').length;
  const unassignedCount = users.filter((u) => u.hospitalId == null).length;

  // Open Edit Modal
  const handleOpenEditModal = (targetUser) => {
    setEditingUser(targetUser);
    setEditFormData({
      role: targetUser.role || 'USER',
      hospitalId: targetUser.hospitalId ? String(targetUser.hospitalId) : '',
      enabled: targetUser.enabled !== undefined ? targetUser.enabled : true,
      password: '',
    });
    setEditFormErrors({});
    setModalError('');
    setIsEditModalOpen(true);
  };

  // Close Edit Modal
  const handleCloseEditModal = () => {
    if (isSaving) return;
    setIsEditModalOpen(false);
    setEditingUser(null);
    setEditFormErrors({});
    setModalError('');
  };

  // Validate Edit Form
  const validateEditForm = () => {
    const errors = {};

    if (!editFormData.role) {
      errors.role = 'Role is required.';
    }

    // If password provided, must be at least 8 chars
    if (editFormData.password && editFormData.password.length < 8) {
      errors.password = 'Password must be at least 8 characters if changing.';
    }

    // Check if selected hospital is disabled
    if (editFormData.hospitalId) {
      const selectedHosp = hospitals.find((h) => String(h.id) === String(editFormData.hospitalId));
      if (selectedHosp && !selectedHosp.enabled) {
        errors.hospitalId = 'Cannot assign user to a disabled hospital facility.';
      }
    }

    setEditFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Submit Edit Form
  const handleSaveEdit = async (e) => {
    e.preventDefault();
    if (!validateEditForm()) return;

    setIsSaving(true);
    setModalError('');

    try {
      const payload = {
        role: editFormData.role,
        enabled: Boolean(editFormData.enabled),
      };

      if (editFormData.hospitalId) {
        payload.hospitalId = Number(editFormData.hospitalId);
      } else if (isPlatformAdmin) {
        // Platform admin selected No Hospital
        payload.clearHospital = true;
      }

      if (editFormData.password && editFormData.password.trim()) {
        payload.password = editFormData.password.trim();
      }

      const updated = await userService.updateUser(editingUser.id, payload);

      // Update state in local list
      setUsers((prev) => prev.map((u) => (u.id === editingUser.id ? { ...u, ...updated } : u)));

      const hospitalDesc = updated.hospitalName
        ? `associated with ${updated.hospitalName} (${updated.hospitalCode})`
        : 'with No Hospital association (Platform-Level)';

      setSuccessMessage(
        `User "${updated.username}" updated successfully with role ${updated.role} ${hospitalDesc}.`
      );

      setIsEditModalOpen(false);
      setEditingUser(null);
    } catch (err) {
      setModalError(err.userMessage || err.message || 'Failed to update user account.');
    } finally {
      setIsSaving(false);
    }
  };

  const formatTimestamp = (ts) => {
    if (!ts) return '—';
    try {
      const d = new Date(ts);
      return isNaN(d.getTime()) ? ts : d.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return ts;
    }
  };

  return (
    <div className="user-management-page">
      {/* Header */}
      <div className="user-management-header">
        <div className="header-titles">
          <div className="breadcrumbs">
            <Link to="/dashboard">Dashboard</Link>
            <span className="separator">/</span>
            <span className="current">User Management</span>
          </div>
          <h1 className="page-title">User Account Administration</h1>
          <p className="page-subtitle">
            Inspect existing clinical and administrative accounts, update tenant associations, and manage permissions.
          </p>
        </div>
        <div className="header-actions">
          <Link to="/users/create">
            <Button variant="primary" size="md">
              <svg viewBox="0 0 20 20" fill="currentColor" style={{ width: 16, height: 16, marginRight: 6 }}>
                <path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd" />
              </svg>
              Register New User
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="user-stats-grid">
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-blue">👥</div>
            <div>
              <span className="stat-label">Total Users</span>
              <h3 className="stat-value">{totalCount}</h3>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-teal">🩺</div>
            <div>
              <span className="stat-label">Clinical Users</span>
              <h3 className="stat-value">{clinicalCount}</h3>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-purple">🛡️</div>
            <div>
              <span className="stat-label">Administrators</span>
              <h3 className="stat-value">{adminCount}</h3>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-amber">⚠️</div>
            <div>
              <span className="stat-label">Unassigned Hospital</span>
              <h3 className="stat-value">{unassignedCount}</h3>
            </div>
          </div>
        </Card>
      </div>

      {/* Alerts */}
      {apiError && (
        <ErrorMessage
          title="Error Loading Users"
          message={apiError}
          onDismiss={() => setApiError('')}
          className="user-alert"
        />
      )}

      {successMessage && (
        <div className="user-success-banner" role="status">
          <div className="success-icon">✓</div>
          <div className="success-text">{successMessage}</div>
          <button type="button" className="close-banner-btn" onClick={() => setSuccessMessage('')}>
            ×
          </button>
        </div>
      )}

      {/* User Listing Card */}
      <Card
        title="Registered System Users"
        subtitle="Tenant-isolated directory of authorized clinical personnel and system administrators"
      >
        {/* Toolbar: Search & Filters */}
        <div className="user-toolbar">
          <div className="toolbar-search">
            <Input
              id="userSearch"
              placeholder="Search by username, hospital, code..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
          </div>

          <div className="toolbar-filters">
            {/* Role Filter */}
            <select
              className="toolbar-select"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              aria-label="Filter by role"
            >
              <option value="ALL">All Roles ({totalCount})</option>
              <option value="USER">Clinical Users ({clinicalCount})</option>
              <option value="ADMIN">Administrators ({adminCount})</option>
            </select>

            {/* Hospital Filter */}
            <select
              className="toolbar-select"
              value={hospitalFilter}
              onChange={(e) => setHospitalFilter(e.target.value)}
              aria-label="Filter by hospital"
            >
              <option value="ALL">All Hospital Tenants</option>
              <option value="UNASSIGNED">Unassigned (No Hospital) ({unassignedCount})</option>
              {hospitals.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} [{h.code}]
                </option>
              ))}
            </select>

            {/* Status Filter */}
            <select
              className="toolbar-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              aria-label="Filter by account status"
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active Only</option>
              <option value="INACTIVE">Inactive Only</option>
            </select>

            <Button variant="secondary" size="sm" onClick={loadData} disabled={isLoading}>
              Refresh
            </Button>
          </div>
        </div>

        {/* Content Area */}
        {isLoading ? (
          <div className="user-loading-container">
            <LoadingSpinner size="lg" message="Loading user directory..." />
          </div>
        ) : filteredUsers.length === 0 ? (
          <EmptyState
            title={
              searchQuery || roleFilter !== 'ALL' || hospitalFilter !== 'ALL' || statusFilter !== 'ALL'
                ? 'No matching users found'
                : 'No users registered yet'
            }
            description={
              searchQuery || roleFilter !== 'ALL' || hospitalFilter !== 'ALL' || statusFilter !== 'ALL'
                ? 'Try adjusting your search criteria or filter options.'
                : 'Get started by registering your first system user.'
            }
            action={
              searchQuery || roleFilter !== 'ALL' || hospitalFilter !== 'ALL' || statusFilter !== 'ALL' ? (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setSearchQuery('');
                    setRoleFilter('ALL');
                    setHospitalFilter('ALL');
                    setStatusFilter('ALL');
                  }}
                >
                  Clear Filters
                </Button>
              ) : (
                <Link to="/users/create">
                  <Button variant="primary" size="sm">
                    Register User
                  </Button>
                </Link>
              )
            }
          />
        ) : (
          <div className="table-responsive">
            <table className="users-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role</th>
                  <th>Assigned Hospital Tenant</th>
                  <th>Status</th>
                  <th>Registered</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((u) => {
                  const hasHospital = u.hospitalId != null;
                  return (
                    <tr key={u.id} className={!hasHospital ? 'unassigned-row' : ''}>
                      <td>
                        <div className="user-name-cell">
                          <div className="user-cell-avatar">
                            {(u.username?.[0] || 'U').toUpperCase()}
                          </div>
                          <div className="user-cell-details">
                            <span className="user-username">{u.username}</span>
                            <span className="user-id-sub">ID: #{u.id}</span>
                          </div>
                        </div>
                      </td>
                      <td>
                        {u.role === 'ADMIN' ? (
                          <span className="role-badge badge-admin">
                            <span className="badge-dot" />
                            ADMIN
                          </span>
                        ) : (
                          <span className="role-badge badge-user">
                            <span className="badge-dot" />
                            USER
                          </span>
                        )}
                      </td>
                      <td>
                        {hasHospital ? (
                          <div className="hospital-tenant-cell">
                            <span className="hospital-tenant-name">{u.hospitalName}</span>
                            <code className="hospital-tenant-code">[{u.hospitalCode}]</code>
                          </div>
                        ) : (
                          <div className="unassigned-hospital-cell">
                            <span className="unassigned-tag">
                              <span className="unassigned-icon">⚠️</span>
                              Unassigned (No Hospital)
                            </span>
                            {u.role === 'USER' && (
                              <span className="unassigned-hint">Requires hospital assignment</span>
                            )}
                          </div>
                        )}
                      </td>
                      <td>
                        <StatusBadge
                          status={u.enabled ? 'ACTIVE' : 'INACTIVE'}
                          variant={u.enabled ? 'success' : 'neutral'}
                        />
                      </td>
                      <td>{formatTimestamp(u.createdAt)}</td>
                      <td style={{ textAlign: 'right' }}>
                        <div className="table-action-group">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setViewingUser(u)}
                            title="Inspect user details"
                          >
                            View
                          </Button>
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleOpenEditModal(u)}
                            title="Edit user role, hospital, or status"
                            className="edit-user-btn"
                          >
                            <svg
                              viewBox="0 0 20 20"
                              fill="currentColor"
                              style={{ width: 14, height: 14, marginRight: 4 }}
                            >
                              <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                            </svg>
                            Edit
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Edit User Modal */}
      {isEditModalOpen && editingUser && (
        <div className="modal-backdrop" onClick={handleCloseEditModal}>
          <div className="modal-card edit-user-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Edit User Account</h3>
                <p className="modal-subtitle">
                  Editing settings for user <strong>{editingUser.username}</strong>
                </p>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseEditModal}
                disabled={isSaving}
                aria-label="Close"
              >
                ×
              </button>
            </div>

            {modalError && (
              <ErrorMessage
                title="Update Failed"
                message={modalError}
                onDismiss={() => setModalError('')}
                className="modal-alert"
              />
            )}

            {/* Unassigned Clinical User Notice */}
            {editingUser.hospitalId == null && (
              <div className="unassigned-notice-box">
                <div className="notice-icon">⚠️</div>
                <div className="notice-text">
                  <strong>Hospital Association Required</strong>
                  <p>
                    This user currently has no hospital tenant association. Assign an active hospital facility below to enable hospital-scoped clinical workflows.
                  </p>
                </div>
              </div>
            )}

            <form onSubmit={handleSaveEdit} className="modal-form" noValidate>
              {/* Username (Preserved / Read-Only) */}
              <div className="input-group">
                <label className="input-label" htmlFor="editUsername">
                  Username
                </label>
                <div className="readonly-input-wrapper">
                  <input
                    id="editUsername"
                    type="text"
                    value={editingUser.username}
                    readOnly
                    disabled
                    className="input-field readonly-field"
                  />
                  <span className="readonly-badge">
                    <svg viewBox="0 0 20 20" fill="currentColor" style={{ width: 14, height: 14 }}>
                      <path
                        fillRule="evenodd"
                        d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                        clipRule="evenodd"
                      />
                    </svg>
                    Permanent
                  </span>
                </div>
                <span className="input-helper">
                  Usernames cannot be modified to maintain audit trail and authentication integrity.
                </span>
              </div>

              {/* Role Selection */}
              <div className="role-selection-group">
                <label className="input-label">
                  Account Role <span className="required-star">*</span>
                </label>
                <div className="role-options-grid">
                  <label className={`role-card ${editFormData.role === 'USER' ? 'selected' : ''}`}>
                    <input
                      type="radio"
                      name="role"
                      value="USER"
                      checked={editFormData.role === 'USER'}
                      onChange={() => setEditFormData({ ...editFormData, role: 'USER' })}
                      disabled={isSaving}
                    />
                    <div className="role-card-content">
                      <div className="role-title-row">
                        <span className="role-name">Clinical User</span>
                        <span className="role-code-badge">USER</span>
                      </div>
                      <p className="role-desc">
                        Standard clinical access to convert HL7, review FHIR outputs, and view patient records.
                      </p>
                    </div>
                  </label>

                  <label className={`role-card ${editFormData.role === 'ADMIN' ? 'selected' : ''}`}>
                    <input
                      type="radio"
                      name="role"
                      value="ADMIN"
                      checked={editFormData.role === 'ADMIN'}
                      onChange={() => setEditFormData({ ...editFormData, role: 'ADMIN' })}
                      disabled={isSaving}
                    />
                    <div className="role-card-content">
                      <div className="role-title-row">
                        <span className="role-name">System Administrator</span>
                        <span className="role-code-badge badge-admin">ADMIN</span>
                      </div>
                      <p className="role-desc">
                        Full administrative access to manage hospital tenants, user accounts, and integrations.
                      </p>
                    </div>
                  </label>
                </div>
              </div>

              {/* Hospital Association Dropdown */}
              <div className="input-group">
                <label htmlFor="editHospitalSelect" className="input-label">
                  Assigned Hospital Tenant
                </label>
                <div className="input-wrapper">
                  <select
                    id="editHospitalSelect"
                    name="hospitalId"
                    className={`input-field ${editFormErrors.hospitalId ? 'input-error' : ''}`}
                    value={editFormData.hospitalId}
                    onChange={(e) => {
                      setEditFormData({ ...editFormData, hospitalId: e.target.value });
                      if (editFormErrors.hospitalId) {
                        setEditFormErrors((prev) => ({ ...prev, hospitalId: null }));
                      }
                    }}
                    disabled={isSaving || !isPlatformAdmin}
                    style={{ cursor: isPlatformAdmin ? 'pointer' : 'not-allowed' }}
                  >
                    {isPlatformAdmin ? (
                      <option value="">-- None / Platform-Level User (No Hospital) --</option>
                    ) : (
                      <option value={editFormData.hospitalId}>
                        {editingUser.hospitalName
                          ? `${editingUser.hospitalName} [${editingUser.hospitalCode}]`
                          : 'Hospital Context'}
                      </option>
                    )}
                    {hospitals.map((h) => (
                      <option key={h.id} value={h.id} disabled={!h.enabled}>
                        {h.name} [{h.code}] {!h.enabled ? '(Disabled Facility)' : ''}
                      </option>
                    ))}
                  </select>
                </div>
                {editFormErrors.hospitalId && (
                  <span className="field-error-text">{editFormErrors.hospitalId}</span>
                )}
                <span className="input-helper">
                  {isPlatformAdmin
                    ? 'Assign to an active hospital facility or leave unassociated for platform-level administrator.'
                    : 'Hospital administrators cannot reassign hospital tenant boundaries.'}
                </span>
              </div>

              {/* Account Status Toggle */}
              <div className="status-toggle-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={editFormData.enabled}
                    onChange={(e) => setEditFormData({ ...editFormData, enabled: e.target.checked })}
                    disabled={isSaving}
                  />
                  <div>
                    <span className="checkbox-title">Account Active</span>
                    <p className="checkbox-desc">
                      When disabled, the user cannot authenticate or obtain JWT tokens.
                    </p>
                  </div>
                </label>
              </div>

              {/* Optional Password Update */}
              <div className="optional-password-section">
                <div className="optional-password-header">
                  <span className="optional-title">Change Password (Optional)</span>
                  <span className="optional-subtitle">Leave blank to preserve existing password hash</span>
                </div>
                <Input
                  id="editPassword"
                  type="password"
                  placeholder="Enter new password (min. 8 chars) or leave blank"
                  value={editFormData.password}
                  onChange={(e) => {
                    setEditFormData({ ...editFormData, password: e.target.value });
                    if (editFormErrors.password) {
                      setEditFormErrors((prev) => ({ ...prev, password: null }));
                    }
                  }}
                  error={editFormErrors.password}
                  disabled={isSaving}
                  autoComplete="new-password"
                />
              </div>

              {/* Modal Actions */}
              <div className="modal-actions">
                <Button
                  type="button"
                  variant="secondary"
                  size="md"
                  onClick={handleCloseEditModal}
                  disabled={isSaving}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  isLoading={isSaving}
                  className="save-user-btn"
                >
                  Save Changes
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View User Modal */}
      {viewingUser && (
        <div className="modal-backdrop" onClick={() => setViewingUser(null)}>
          <div className="modal-card view-user-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">User Account Details</h3>
                <p className="modal-subtitle">User #{viewingUser.id}</p>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setViewingUser(null)}
                aria-label="Close"
              >
                ×
              </button>
            </div>

            <div className="view-user-body">
              <div className="view-user-avatar-hero">
                <div className="hero-avatar">
                  {(viewingUser.username?.[0] || 'U').toUpperCase()}
                </div>
                <div>
                  <h4 className="hero-username">{viewingUser.username}</h4>
                  <span className="hero-role-badge">
                    {viewingUser.role === 'ADMIN' ? 'System Administrator' : 'Clinical User'}
                  </span>
                </div>
              </div>

              <div className="view-details-grid">
                <div className="detail-item">
                  <span className="detail-label">Username</span>
                  <span className="detail-value">{viewingUser.username}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Role</span>
                  <span className="detail-value">
                    <code className="detail-code">{viewingUser.role}</code>
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Account Status</span>
                  <span className="detail-value">
                    <StatusBadge
                      status={viewingUser.enabled ? 'ACTIVE' : 'INACTIVE'}
                      variant={viewingUser.enabled ? 'success' : 'neutral'}
                    />
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Hospital Tenant</span>
                  <span className="detail-value">
                    {viewingUser.hospitalName ? (
                      <>
                        {viewingUser.hospitalName}{' '}
                        <code className="hospital-code-pill">{viewingUser.hospitalCode}</code>
                      </>
                    ) : (
                      <span className="text-muted">None (Platform-Level / No Hospital)</span>
                    )}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Hospital ID</span>
                  <span className="detail-value">
                    {viewingUser.hospitalId != null ? viewingUser.hospitalId : '—'}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Registered Date</span>
                  <span className="detail-value">{formatTimestamp(viewingUser.createdAt)}</span>
                </div>
              </div>
            </div>

            <div className="modal-actions">
              <Button
                variant="primary"
                size="md"
                onClick={() => {
                  const target = viewingUser;
                  setViewingUser(null);
                  handleOpenEditModal(target);
                }}
              >
                Edit This User
              </Button>
              <Button variant="secondary" size="md" onClick={() => setViewingUser(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
