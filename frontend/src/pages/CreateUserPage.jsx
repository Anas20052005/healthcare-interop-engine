import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/Card';
import Input from '../components/Input';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import userService from '../services/userService';
import hospitalService from '../services/hospitalService';
import { useAuth } from '../context/useAuth';
import './CreateUserPage.css';

export default function CreateUserPage() {
  const { user } = useAuth();
  const isPlatformAdmin = user?.role === 'ADMIN' && !user?.hospitalId;

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState('USER');
  const [hospitalId, setHospitalId] = useState(user?.hospitalId ? String(user.hospitalId) : '');
  const [showPassword, setShowPassword] = useState(false);

  const [hospitals, setHospitals] = useState([]);
  const [isLoadingHospitals, setIsLoadingHospitals] = useState(true);

  const [isLoading, setIsLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [successData, setSuccessData] = useState(null);

  // Load existing hospitals on mount
  useEffect(() => {
    let isMounted = true;
    async function loadHospitals() {
      try {
        setIsLoadingHospitals(true);
        const data = await hospitalService.getHospitals();
        if (isMounted) {
          setHospitals(Array.isArray(data) ? data : []);
          if (!isPlatformAdmin && user?.hospitalId) {
            setHospitalId(String(user.hospitalId));
          }
        }
      } catch (err) {
        console.error('Failed to load hospitals for selection:', err);
      } finally {
        if (isMounted) {
          setIsLoadingHospitals(false);
        }
      }
    }
    loadHospitals();
    return () => {
      isMounted = false;
    };
  }, [isPlatformAdmin, user?.hospitalId]);

  const validate = () => {
    const errors = {};

    if (!username.trim()) {
      errors.username = 'Username is required.';
    } else if (username.trim().length < 3) {
      errors.username = 'Username must be at least 3 characters long.';
    } else if (!/^[a-zA-Z0-9_.-]+$/.test(username.trim())) {
      errors.username = 'Only alphanumeric characters, underscores, hyphens, and dots allowed.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    } else if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters long.';
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Confirmation password is required.';
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    setSuccessData(null);

    if (!validate()) {
      return;
    }

    setIsLoading(true);

    try {
      const response = await userService.createUser({
        username: username.trim(),
        password,
        role,
        hospitalId: hospitalId ? Number(hospitalId) : null,
      });

      setSuccessData(response);
      setUsername('');
      setPassword('');
      setConfirmPassword('');
      setRole('USER');
      setHospitalId('');
      setFieldErrors({});
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to create user. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    setUsername('');
    setPassword('');
    setConfirmPassword('');
    setRole('USER');
    setHospitalId('');
    setFieldErrors({});
    setApiError('');
    setSuccessData(null);
  };

  return (
    <div className="create-user-page">
      {/* Page Header */}
      <div className="create-user-header">
        <div className="header-titles">
          <div className="breadcrumbs">
            <Link to="/dashboard">Dashboard</Link>
            <span className="separator">/</span>
            <Link to="/users">User Management</Link>
            <span className="separator">/</span>
            <span className="current">Register User</span>
          </div>
          <h1 className="page-title">Register New User / Admin</h1>
          <p className="page-subtitle">
            Provision authenticated user accounts with optional hospital tenant association.
          </p>
        </div>
        <div className="header-actions">
          <Link to="/users">
            <Button variant="secondary" size="md">
              ← Back to Users
            </Button>
          </Link>
        </div>
      </div>

      <div className="create-user-container">
        {/* Form Card */}
        <Card
          title="Account Information"
          subtitle="All credentials are encrypted with BCrypt prior to persistence in compliance with HIPAA"
          className="create-user-card"
        >
          {apiError && (
            <ErrorMessage
              title="Creation Error"
              message={apiError}
              onDismiss={() => setApiError('')}
              className="user-alert"
            />
          )}

          {successData && (
            <div className="user-success-banner" role="status">
              <div className="success-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
              </div>
              <div className="success-content">
                <h4 className="success-title">Account Created Successfully</h4>
                <p className="success-details">
                  User <strong>{successData.username}</strong> has been provisioned with role{' '}
                  <span className="role-tag">{successData.role}</span>
                  {successData.hospitalName ? (
                    <> associated with <strong>{successData.hospitalName}</strong> ({successData.hospitalCode})</>
                  ) : (
                    <> (Platform-Level / No Hospital)</>
                  )}.
                </p>
              </div>
              <button
                type="button"
                className="close-success-btn"
                onClick={() => setSuccessData(null)}
                aria-label="Dismiss"
              >
                ×
              </button>
            </div>
          )}

          <form onSubmit={handleSubmit} className="create-user-form" noValidate>
            {/* Username Input */}
            <Input
              id="username"
              name="username"
              label="Username"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                if (fieldErrors.username) setFieldErrors((prev) => ({ ...prev, username: null }));
              }}
              placeholder="e.g. hospitaladmin or clinicaluser"
              required
              error={fieldErrors.username}
              helperText="Unique username used for login"
              disabled={isLoading}
              autoComplete="off"
            />

            {/* Hospital Association Selector */}
            <div className="input-group">
              <label htmlFor="hospitalSelect" className="input-label">
                Assigned Hospital Tenant
              </label>
              <div className="input-wrapper">
                <select
                  id="hospitalSelect"
                  name="hospitalId"
                  className="input-field"
                  value={hospitalId}
                  onChange={(e) => setHospitalId(e.target.value)}
                  disabled={isLoading || isLoadingHospitals || !isPlatformAdmin}
                  style={{ cursor: isPlatformAdmin ? 'pointer' : 'not-allowed' }}
                >
                  {isPlatformAdmin && (
                    <option value="">-- None / Platform-Level User (No Hospital) --</option>
                  )}
                  {hospitals.map((h) => (
                    <option key={h.id} value={h.id} disabled={!h.enabled}>
                      {h.name} [{h.code}] {!h.enabled ? '(Disabled)' : ''}
                    </option>
                  ))}
                </select>
              </div>
              <span className="input-helper" style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                {isLoadingHospitals
                  ? 'Loading registered hospitals from backend...'
                  : isPlatformAdmin
                  ? 'Select an active hospital or leave blank for platform-level administrator'
                  : 'Account will be associated with your assigned hospital tenant'}
              </span>
            </div>

            {/* Role Selection */}
            <div className="role-selection-group">
              <label className="input-label">
                Account Role <span className="required-star">*</span>
              </label>
              <div className="role-options-grid">
                <label className={`role-card ${role === 'USER' ? 'selected' : ''}`}>
                  <input
                    type="radio"
                    name="role"
                    value="USER"
                    checked={role === 'USER'}
                    onChange={() => setRole('USER')}
                    disabled={isLoading}
                  />
                  <div className="role-card-content">
                    <div className="role-title-row">
                      <span className="role-name">Clinical User</span>
                      <span className="role-code-badge">USER</span>
                    </div>
                    <p className="role-desc">
                      Standard access to convert HL7 messages, inspect FHIR outputs, and view conversion history.
                    </p>
                  </div>
                </label>

                <label className={`role-card ${role === 'ADMIN' ? 'selected' : ''}`}>
                  <input
                    type="radio"
                    name="role"
                    value="ADMIN"
                    checked={role === 'ADMIN'}
                    onChange={() => setRole('ADMIN')}
                    disabled={isLoading}
                  />
                  <div className="role-card-content">
                    <div className="role-title-row">
                      <span className="role-name">System Administrator</span>
                      <span className="role-code-badge badge-admin">ADMIN</span>
                    </div>
                    <p className="role-desc">
                      Full administrative access, including registering hospitals, users, and managing integrations.
                    </p>
                  </div>
                </label>
              </div>
            </div>

            {/* Password Grid */}
            <div className="passwords-grid">
              <Input
                id="password"
                name="password"
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: null }));
                }}
                placeholder="At least 8 characters"
                required
                error={fieldErrors.password}
                helperText="Must be 8+ characters long"
                disabled={isLoading}
                autoComplete="new-password"
                rightElement={
                  <button
                    type="button"
                    className="pwd-toggle-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? (
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                        <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.11 2.454-.303z" />
                      </svg>
                    ) : (
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                        <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
                      </svg>
                    )}
                  </button>
                }
              />

              <Input
                id="confirmPassword"
                name="confirmPassword"
                label="Confirm Password"
                type={showPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (fieldErrors.confirmPassword) setFieldErrors((prev) => ({ ...prev, confirmPassword: null }));
                }}
                placeholder="Re-enter password"
                required
                error={fieldErrors.confirmPassword}
                helperText="Must match password"
                disabled={isLoading}
                autoComplete="new-password"
              />
            </div>

            {/* Form Actions */}
            <div className="form-action-row">
              <Button
                type="submit"
                variant="primary"
                size="md"
                isLoading={isLoading}
                className="submit-create-btn"
              >
                Create Account
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="md"
                onClick={handleReset}
                disabled={isLoading}
              >
                Reset
              </Button>
              <Link to="/users" className="cancel-link">
                Cancel
              </Link>
            </div>
          </form>
        </Card>

        {/* Security Info Card */}
        <div className="security-guide-column">
          <Card
            title="HIPAA Security Notice"
            subtitle="Security standards for clinical identity"
            className="security-notice-card"
          >
            <div className="security-guidelines">
              <div className="guideline-item">
                <div className="guideline-icon">🔐</div>
                <div>
                  <strong>BCrypt Password Hashing</strong>
                  <p>All passwords are salted and hashed using BCrypt. Raw passwords are never stored or logged.</p>
                </div>
              </div>
              <div className="guideline-item">
                <div className="guideline-icon">🏥</div>
                <div>
                  <strong>Hospital Association</strong>
                  <p>Users can be linked directly to a hospital tenant for audit traceability and organizational boundaries.</p>
                </div>
              </div>
              <div className="guideline-item">
                <div className="guideline-icon">🛡️</div>
                <div>
                  <strong>Role-Based Access (RBAC)</strong>
                  <p>Only users with active ADMIN credentials are authorized to invoke account creation APIs.</p>
                </div>
              </div>
              <div className="guideline-item">
                <div className="guideline-icon">⏱️</div>
                <div>
                  <strong>Stateless JWT</strong>
                  <p>Upon registration, the newly created user can log in to obtain a cryptographically signed Bearer JWT.</p>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
