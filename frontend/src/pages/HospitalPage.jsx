import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/Card';
import Input from '../components/Input';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import hospitalService from '../services/hospitalService';
import './HospitalPage.css';

export default function HospitalPage() {
  const [hospitals, setHospitals] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal / Detail States
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingHospitalId, setEditingHospitalId] = useState(null);
  const [viewingHospital, setViewingHospital] = useState(null);

  // Form State
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    email: '',
    phone: '',
    address: '',
    enabled: true,
  });
  const [formErrors, setFormErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);

  const loadHospitals = useCallback(async () => {
    setIsLoading(true);
    setApiError('');
    try {
      const data = await hospitalService.getHospitals();
      setHospitals(Array.isArray(data) ? data : []);
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to retrieve hospitals.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    async function fetchInitial() {
      try {
        const data = await hospitalService.getHospitals();
        if (!ignore) {
          setHospitals(Array.isArray(data) ? data : []);
          setIsLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          setApiError(err.userMessage || err.message || 'Failed to retrieve hospitals.');
          setIsLoading(false);
        }
      }
    }
    fetchInitial();
    return () => {
      ignore = true;
    };
  }, []);

  // Filtered List
  const filteredHospitals = useMemo(() => {
    return hospitals.filter((h) => {
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && h.enabled) ||
        (statusFilter === 'INACTIVE' && !h.enabled);

      const q = searchQuery.toLowerCase().trim();
      const matchesQuery =
        !q ||
        h.name.toLowerCase().includes(q) ||
        h.code.toLowerCase().includes(q) ||
        (h.email && h.email.toLowerCase().includes(q)) ||
        (h.address && h.address.toLowerCase().includes(q));

      return matchesStatus && matchesQuery;
    });
  }, [hospitals, searchQuery, statusFilter]);

  // Statistics
  const totalCount = hospitals.length;
  const activeCount = hospitals.filter((h) => h.enabled).length;
  const inactiveCount = totalCount - activeCount;

  // Open Create Modal
  const handleOpenCreateModal = () => {
    setEditingHospitalId(null);
    setFormData({
      name: '',
      code: '',
      email: '',
      phone: '',
      address: '',
      enabled: true,
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  // Open Edit Modal
  const handleOpenEditModal = (h) => {
    setEditingHospitalId(h.id);
    setFormData({
      name: h.name || '',
      code: h.code || '',
      email: h.email || '',
      phone: h.phone || '',
      address: h.address || '',
      enabled: h.enabled !== undefined ? h.enabled : true,
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  // Close Form Modal
  const handleCloseModal = () => {
    if (isSaving) return;
    setIsModalOpen(false);
    setEditingHospitalId(null);
    setFormErrors({});
  };

  // Form Validation
  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) {
      errors.name = 'Hospital name is required.';
    } else if (formData.name.trim().length < 2) {
      errors.name = 'Hospital name must be at least 2 characters.';
    }

    if (!formData.code.trim()) {
      errors.code = 'Hospital unique code is required.';
    } else if (!/^[a-zA-Z0-9_.-]+$/.test(formData.code.trim())) {
      errors.code = 'Code can only contain letters, numbers, underscores, hyphens, and dots.';
    }

    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errors.email = 'Please enter a valid email address.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Submit Form
  const handleSubmitForm = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSaving(true);
    setApiError('');

    try {
      if (editingHospitalId) {
        const updated = await hospitalService.updateHospital(editingHospitalId, formData);
        setHospitals((prev) => prev.map((h) => (h.id === editingHospitalId ? updated : h)));
        setSuccessMessage(`Hospital "${updated.name}" updated successfully.`);
      } else {
        const created = await hospitalService.createHospital(formData);
        setHospitals((prev) => [created, ...prev]);
        setSuccessMessage(`Hospital "${created.name}" (${created.code}) created successfully.`);
      }

      setIsModalOpen(false);
      setEditingHospitalId(null);
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Operation failed.');
    } finally {
      setIsSaving(false);
    }
  };

  const formatTimestamp = (ts) => {
    if (!ts) return '—';
    try {
      const d = new Date(ts);
      return isNaN(d.getTime()) ? ts : d.toLocaleDateString();
    } catch {
      return ts;
    }
  };

  return (
    <div className="hospital-page">
      {/* Page Header */}
      <div className="hospital-header">
        <div className="header-titles">
          <div className="breadcrumbs">
            <Link to="/dashboard">Dashboard</Link>
            <span className="separator">/</span>
            <span className="current">Hospital Management</span>
          </div>
          <h1 className="page-title">Hospital Tenant Management</h1>
          <p className="page-subtitle">
            Configure registered healthcare facilities, codes, and multi-tenant clinical organizations.
          </p>
        </div>
        <div className="header-actions">
          <Button variant="primary" size="md" onClick={handleOpenCreateModal}>
            <svg viewBox="0 0 20 20" fill="currentColor" style={{ width: 16, height: 16, marginRight: 6 }}>
              <path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd" />
            </svg>
            Register Hospital
          </Button>
        </div>
      </div>

      {/* KPI Stats Cards */}
      <div className="hospital-stats-grid">
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-blue">🏥</div>
            <div>
              <span className="stat-label">Total Hospitals</span>
              <h3 className="stat-value">{totalCount}</h3>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-green">✅</div>
            <div>
              <span className="stat-label">Active Facilities</span>
              <h3 className="stat-value">{activeCount}</h3>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="stat-card-inner">
            <div className="stat-icon icon-amber">⏸️</div>
            <div>
              <span className="stat-label">Inactive / Disabled</span>
              <h3 className="stat-value">{inactiveCount}</h3>
            </div>
          </div>
        </Card>
      </div>

      {/* Alerts */}
      {apiError && (
        <ErrorMessage
          title="Error"
          message={apiError}
          onDismiss={() => setApiError('')}
          className="hospital-alert"
        />
      )}

      {successMessage && (
        <div className="hospital-success-banner" role="status">
          <div className="success-icon">✓</div>
          <div className="success-text">{successMessage}</div>
          <button type="button" className="close-banner-btn" onClick={() => setSuccessMessage('')}>
            ×
          </button>
        </div>
      )}

      {/* Main Table Card */}
      <Card title="Registered Hospitals" subtitle="Manage healthcare facility identities and access states">
        {/* Filter and Search Bar */}
        <div className="hospital-toolbar">
          <div className="toolbar-search">
            <Input
              id="hospitalSearch"
              placeholder="Search by hospital name, code, email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
          </div>
          <div className="toolbar-filter">
            <select
              className="status-filter-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses ({totalCount})</option>
              <option value="ACTIVE">Active Only ({activeCount})</option>
              <option value="INACTIVE">Inactive Only ({inactiveCount})</option>
            </select>
            <Button variant="secondary" size="sm" onClick={loadHospitals} disabled={isLoading}>
              Refresh
            </Button>
          </div>
        </div>

        {/* Content Area */}
        {isLoading ? (
          <div className="hospital-loading-container">
            <LoadingSpinner size="lg" message="Loading registered hospitals..." />
          </div>
        ) : filteredHospitals.length === 0 ? (
          <EmptyState
            title={searchQuery || statusFilter !== 'ALL' ? 'No matching hospitals found' : 'No hospitals registered yet'}
            description={
              searchQuery || statusFilter !== 'ALL'
                ? 'Try adjusting your search criteria or status filter.'
                : 'Click "Register Hospital" to provision your first healthcare tenant.'
            }
            action={
              searchQuery || statusFilter !== 'ALL' ? (
                <Button variant="secondary" size="sm" onClick={() => { setSearchQuery(''); setStatusFilter('ALL'); }}>
                  Clear Filters
                </Button>
              ) : (
                <Button variant="primary" size="sm" onClick={handleOpenCreateModal}>
                  Register First Hospital
                </Button>
              )
            }
          />
        ) : (
          <div className="table-responsive">
            <table className="hospitals-table">
              <thead>
                <tr>
                  <th>Hospital Name</th>
                  <th>Code</th>
                  <th>Status</th>
                  <th>Contact Email</th>
                  <th>Phone</th>
                  <th>Registered</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredHospitals.map((h) => (
                  <tr key={h.id}>
                    <td>
                      <div className="hospital-name-cell">
                        <strong>{h.name}</strong>
                        {h.address && <span className="hospital-address-sub">{h.address}</span>}
                      </div>
                    </td>
                    <td>
                      <code className="hospital-code-pill">{h.code}</code>
                    </td>
                    <td>
                      <StatusBadge
                        status={h.enabled ? 'ACTIVE' : 'INACTIVE'}
                        variant={h.enabled ? 'success' : 'warning'}
                      />
                    </td>
                    <td>{h.email || <span className="text-muted">—</span>}</td>
                    <td>{h.phone || <span className="text-muted">—</span>}</td>
                    <td>{formatTimestamp(h.createdAt)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <div className="table-action-group">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setViewingHospital(h)}
                          title="View details"
                        >
                          View
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenEditModal(h)}
                          title="Edit hospital"
                        >
                          Edit
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Create / Edit Hospital Modal */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={handleCloseModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">
                {editingHospitalId ? 'Edit Hospital Details' : 'Register New Hospital'}
              </h3>
              <button type="button" className="modal-close-btn" onClick={handleCloseModal} disabled={isSaving}>
                ×
              </button>
            </div>

            <form onSubmit={handleSubmitForm} className="modal-form" noValidate>
              <Input
                id="hospitalName"
                label="Hospital Name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. St. Jude Medical Center"
                required
                error={formErrors.name}
                disabled={isSaving}
              />

              <Input
                id="hospitalCode"
                label="Unique Hospital Code"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                placeholder="e.g. STJUDE_01"
                required
                error={formErrors.code}
                helperText="Alphanumeric identifier used across HL7 message MSH headers"
                disabled={isSaving}
              />

              <div className="form-row-2">
                <Input
                  id="hospitalEmail"
                  label="Contact Email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  placeholder="admin@hospital.org"
                  error={formErrors.email}
                  disabled={isSaving}
                />
                <Input
                  id="hospitalPhone"
                  label="Contact Phone"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  placeholder="555-0100"
                  disabled={isSaving}
                />
              </div>

              <Input
                id="hospitalAddress"
                label="Facility Address"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                placeholder="100 Hospital Way, Springfield, IL"
                disabled={isSaving}
              />

              <div className="form-checkbox-row">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.enabled}
                    onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                    disabled={isSaving}
                  />
                  <span>Active / Enabled (allows assigning clinical users and processing messages)</span>
                </label>
              </div>

              <div className="modal-actions">
                <Button type="button" variant="secondary" onClick={handleCloseModal} disabled={isSaving}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" isLoading={isSaving}>
                  {editingHospitalId ? 'Save Changes' : 'Register Hospital'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View Hospital Details Modal */}
      {viewingHospital && (
        <div className="modal-backdrop" onClick={() => setViewingHospital(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Hospital Tenant Details</h3>
              <button type="button" className="modal-close-btn" onClick={() => setViewingHospital(null)}>
                ×
              </button>
            </div>
            <div className="detail-view-body">
              <div className="detail-header-row">
                <div>
                  <h4 className="detail-hospital-name">{viewingHospital.name}</h4>
                  <code className="hospital-code-pill large">{viewingHospital.code}</code>
                </div>
                <StatusBadge
                  status={viewingHospital.enabled ? 'ACTIVE' : 'INACTIVE'}
                  variant={viewingHospital.enabled ? 'success' : 'warning'}
                />
              </div>

              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-label">Database ID</span>
                  <span className="detail-value">{viewingHospital.id}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Status</span>
                  <span className="detail-value">{viewingHospital.enabled ? 'Operational (Enabled)' : 'Disabled'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Contact Email</span>
                  <span className="detail-value">{viewingHospital.email || '—'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Contact Phone</span>
                  <span className="detail-value">{viewingHospital.phone || '—'}</span>
                </div>
                <div className="detail-item full-width">
                  <span className="detail-label">Facility Address</span>
                  <span className="detail-value">{viewingHospital.address || '—'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Created Date</span>
                  <span className="detail-value">{formatTimestamp(viewingHospital.createdAt)}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Last Updated</span>
                  <span className="detail-value">{formatTimestamp(viewingHospital.updatedAt)}</span>
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <Button
                variant="outline"
                onClick={() => {
                  const target = viewingHospital;
                  setViewingHospital(null);
                  handleOpenEditModal(target);
                }}
              >
                Edit Hospital
              </Button>
              <Button variant="secondary" onClick={() => setViewingHospital(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
