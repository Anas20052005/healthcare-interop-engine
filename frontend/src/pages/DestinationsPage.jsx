import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Card from '../components/Card';
import Input from '../components/Input';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import destinationService from '../services/destinationService';
import './DestinationsPage.css';

export default function DestinationsPage() {
  const [activeTab, setActiveTab] = useState('destinations'); // 'destinations' | 'mock-inspector'
  const [destinations, setDestinations] = useState([]);
  const [mockMessages, setMockMessages] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isMockLoading, setIsMockLoading] = useState(false);
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Destination Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, ACTIVE, INACTIVE

  // Create / Edit Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDestinationId, setEditingDestinationId] = useState(null);
  const [showTokenInput, setShowTokenInput] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    baseUrl: '',
    fhirVersion: 'R4',
    authType: 'NONE',
    authToken: '',
    enabled: true,
  });
  const [formErrors, setFormErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);

  // Deliveries Modal State
  const [deliveriesModalOpen, setDeliveriesModalOpen] = useState(false);
  const [selectedDestinationForDeliveries, setSelectedDestinationForDeliveries] = useState(null);
  const [destinationDeliveries, setDestinationDeliveries] = useState([]);
  const [isDeliveriesLoading, setIsDeliveriesLoading] = useState(false);

  // Mock Payload Modal State
  const [viewingMockResource, setViewingMockResource] = useState(null);

  // Connection Test State
  const [testingDestinationId, setTestingDestinationId] = useState(null);
  const [testResults, setTestResults] = useState({}); // { [destId]: { success, message, latencyMs, serverSoftware, serverFhirVersion, httpStatusCode } }

  // Initial load
  const loadDestinations = useCallback(async (showLoading = false) => {
    if (showLoading) {
      setIsLoading(true);
      setApiError('');
    }
    try {
      const data = await destinationService.getDestinations();
      setDestinations(Array.isArray(data) ? data : []);
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to load destinations.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadMockMessages = useCallback(async (showLoading = false) => {
    if (showLoading) {
      setIsMockLoading(true);
    }
    try {
      const data = await destinationService.getMockReceivedMessages();
      setMockMessages(Array.isArray(data) ? data : []);
    } catch (err) {
      // Mock messages failure should not block entire page
      console.error('Failed to load mock server messages', err);
    } finally {
      setIsMockLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    async function fetchInitialData() {
      try {
        const [destData, mockData] = await Promise.allSettled([
          destinationService.getDestinations(),
          destinationService.getMockReceivedMessages(),
        ]);
        if (ignore) return;
        if (destData.status === 'fulfilled') {
          setDestinations(Array.isArray(destData.value) ? destData.value : []);
        } else {
          setApiError(destData.reason?.userMessage || destData.reason?.message || 'Failed to load destinations.');
        }
        if (mockData.status === 'fulfilled') {
          setMockMessages(Array.isArray(mockData.value) ? mockData.value : []);
        }
      } catch (err) {
        if (!ignore) {
          console.error('Failed to load initial destination data', err);
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
          setIsMockLoading(false);
        }
      }
    }
    fetchInitialData();
    return () => {
      ignore = true;
    };
  }, []);

  const clearNotifications = () => {
    setApiError('');
    setSuccessMessage('');
  };

  // Filtered Destinations
  const filteredDestinations = useMemo(() => {
    return destinations.filter((dest) => {
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && dest.enabled) ||
        (statusFilter === 'INACTIVE' && !dest.enabled);

      const q = searchQuery.toLowerCase().trim();
      const matchesQuery =
        !q ||
        dest.name.toLowerCase().includes(q) ||
        dest.baseUrl.toLowerCase().includes(q) ||
        (dest.hospitalName && dest.hospitalName.toLowerCase().includes(q));

      return matchesStatus && matchesQuery;
    });
  }, [destinations, searchQuery, statusFilter]);

  // Open Create Modal
  const handleOpenCreateModal = () => {
    clearNotifications();
    setEditingDestinationId(null);
    setFormData({
      name: '',
      baseUrl: '',
      fhirVersion: 'R4',
      authType: 'NONE',
      authToken: '',
      enabled: true,
    });
    setFormErrors({});
    setShowTokenInput(false);
    setIsModalOpen(true);
  };

  // Open Edit Modal
  const handleOpenEditModal = (dest) => {
    clearNotifications();
    setEditingDestinationId(dest.id);
    setFormData({
      name: dest.name,
      baseUrl: dest.baseUrl,
      fhirVersion: dest.fhirVersion || 'R4',
      authType: dest.authType || 'NONE',
      authToken: '', // Kept empty; user leaves empty to retain existing secret
      enabled: dest.enabled,
    });
    setFormErrors({});
    setShowTokenInput(false);
    setIsModalOpen(true);
  };

  // Fill Local Mock Server defaults
  const handleFillMockServerDefaults = () => {
    setFormData((prev) => ({
      ...prev,
      name: 'Local Mock FHIR Server (Dev/Test)',
      baseUrl: 'http://localhost:8080/api/mock-fhir',
      fhirVersion: 'R4',
      authType: 'NONE',
      authToken: '',
      enabled: true,
    }));
    setFormErrors({});
  };

  // Validate form
  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) {
      errors.name = 'Destination name is required.';
    }
    if (!formData.baseUrl.trim()) {
      errors.baseUrl = 'Base URL is required.';
    } else {
      try {
        const url = new URL(formData.baseUrl.trim());
        if (url.protocol !== 'http:' && url.protocol !== 'https:') {
          errors.baseUrl = 'URL must start with http:// or https://';
        }
      } catch {
        errors.baseUrl = 'Enter a valid URL (e.g., https://fhir.hospital.org/r4).';
      }
    }
    if (formData.authType === 'BEARER_TOKEN' && !editingDestinationId && !formData.authToken.trim()) {
      errors.authToken = 'Auth token is required when Bearer Token authentication is selected.';
    }
    return errors;
  };

  // Save Destination
  const handleSaveDestination = async (e) => {
    e.preventDefault();
    clearNotifications();
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setIsSaving(true);
    try {
      if (editingDestinationId) {
        const payload = {
          name: formData.name.trim(),
          baseUrl: formData.baseUrl.trim(),
          fhirVersion: formData.fhirVersion,
          authType: formData.authType,
          enabled: formData.enabled,
        };
        // Backend DestinationUpdateRequest expects field name 'bearerToken'
        if (formData.authType === 'BEARER_TOKEN' && formData.authToken.trim()) {
          payload.bearerToken = formData.authToken.trim();
        }
        await destinationService.updateDestination(editingDestinationId, payload);
        setSuccessMessage(`Destination "${formData.name}" updated successfully.`);
      } else {
        const payload = {
          name: formData.name.trim(),
          baseUrl: formData.baseUrl.trim(),
          fhirVersion: formData.fhirVersion,
          authType: formData.authType,
          // Backend DestinationCreateRequest expects field name 'bearerToken'
          bearerToken: formData.authType === 'BEARER_TOKEN' ? formData.authToken.trim() : null,
          enabled: formData.enabled,
        };
        await destinationService.createDestination(payload);
        setSuccessMessage(`Destination "${formData.name}" created successfully.`);
      }
      setIsModalOpen(false);
      await loadDestinations();
    } catch (err) {
      setFormErrors({ submit: err.userMessage || err.message || 'Failed to save destination.' });
    } finally {
      setIsSaving(false);
    }
  };

  // Toggle Status
  const handleToggleStatus = async (dest) => {
    clearNotifications();
    const newStatus = !dest.enabled;
    try {
      await destinationService.updateDestinationStatus(dest.id, newStatus);
      setSuccessMessage(`Destination "${dest.name}" ${newStatus ? 'enabled' : 'disabled'} successfully.`);
      await loadDestinations();
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to update destination status.');
    }
  };

  // Delete Destination
  const handleDeleteDestination = async (dest) => {
    clearNotifications();
    if (!window.confirm(`Are you sure you want to delete destination "${dest.name}"? This will not remove past conversion delivery audit history.`)) {
      return;
    }
    try {
      await destinationService.deleteDestination(dest.id);
      setSuccessMessage(`Destination "${dest.name}" deleted successfully.`);
      // Clear any cached test result for this destination
      setTestResults((prev) => { const n = { ...prev }; delete n[dest.id]; return n; });
      await loadDestinations();
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to delete destination.');
    }
  };

  // Test Connection
  const handleTestConnection = async (dest) => {
    setTestingDestinationId(dest.id);
    // Clear previous result for this dest
    setTestResults((prev) => { const n = { ...prev }; delete n[dest.id]; return n; });
    try {
      const result = await destinationService.testConnection(dest.id);
      setTestResults((prev) => ({ ...prev, [dest.id]: result }));
    } catch (err) {
      setTestResults((prev) => ({
        ...prev,
        [dest.id]: {
          success: false,
          message: err.userMessage || err.message || 'Connection test failed.',
          latencyMs: null,
          httpStatusCode: null,
        },
      }));
    } finally {
      setTestingDestinationId(null);
    }
  };

  // View Deliveries
  const handleViewDeliveries = async (dest) => {
    setSelectedDestinationForDeliveries(dest);
    setDeliveriesModalOpen(true);
    setIsDeliveriesLoading(true);
    try {
      const data = await destinationService.getDestinationDeliveries(dest.id);
      setDestinationDeliveries(Array.isArray(data) ? data : []);
    } catch {
      setDestinationDeliveries([]);
    } finally {
      setIsDeliveriesLoading(false);
    }
  };

  // Clear Mock Buffer
  const handleClearMockMessages = async () => {
    if (!window.confirm('Clear all received mock FHIR server messages from memory?')) return;
    try {
      await destinationService.clearMockReceivedMessages();
      setMockMessages([]);
      setSuccessMessage('Mock FHIR destination message buffer cleared.');
    } catch (err) {
      setApiError(err.userMessage || err.message || 'Failed to clear mock server buffer.');
    }
  };

  return (
    <div className="destinations-page-container">
      {/* Page Header */}
      <div className="destinations-header">
        <div>
          <h1 className="destinations-title">FHIR Destinations & Mock Server</h1>
          <p className="destinations-subtitle">
            Configure external FHIR endpoints, manage secure token credentials, and inspect payloads dispatched to the embedded Mock FHIR Server.
          </p>
        </div>
        <div className="destinations-header-actions">
          <Button variant="secondary" onClick={() => { loadDestinations(); loadMockMessages(); }}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="btn-icon">
              <path d="M23 4v6h-6" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            Refresh
          </Button>
          <Button variant="primary" onClick={handleOpenCreateModal}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="btn-icon">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            New Destination
          </Button>
        </div>
      </div>

      {/* Notifications */}
      {apiError && <ErrorMessage message={apiError} onDismiss={() => setApiError('')} />}
      {successMessage && (
        <div className="destinations-alert-success">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          <span>{successMessage}</span>
          <button className="alert-close-btn" onClick={() => setSuccessMessage('')}>&times;</button>
        </div>
      )}

      {/* Tabs Navigation */}
      <div className="destinations-tabs">
        <button
          className={`dest-tab-btn ${activeTab === 'destinations' ? 'active' : ''}`}
          onClick={() => setActiveTab('destinations')}
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="2" y="2" width="20" height="8" rx="2" ry="2" />
            <rect x="2" y="14" width="20" height="8" rx="2" ry="2" />
            <line x1="6" y1="6" x2="6.01" y2="6" />
            <line x1="6" y1="18" x2="6.01" y2="18" />
          </svg>
          Destinations ({destinations.length})
        </button>
        <button
          className={`dest-tab-btn ${activeTab === 'mock-inspector' ? 'active' : ''}`}
          onClick={() => { setActiveTab('mock-inspector'); loadMockMessages(); }}
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
          Mock FHIR Inspector ({mockMessages.length})
        </button>
      </div>

      {/* TAB 1: DESTINATIONS */}
      {activeTab === 'destinations' && (
        <>
          {/* Summary Stats */}
          <div className="dest-stats-grid">
            <Card className="dest-stat-card">
              <div className="stat-title">Configured Destinations</div>
              <div className="stat-value">{destinations.length}</div>
              <div className="stat-subtext">Total available in scope</div>
            </Card>
            <Card className="dest-stat-card">
              <div className="stat-title">Active Destinations</div>
              <div className="stat-value text-success">
                {destinations.filter((d) => d.enabled).length}
              </div>
              <div className="stat-subtext">Ready to receive FHIR resources</div>
            </Card>
            <Card className="dest-stat-card">
              <div className="stat-title">Platform & Mock Destinations</div>
              <div className="stat-value text-info">
                {destinations.filter((d) => d.platformWide || d.mock).length}
              </div>
              <div className="stat-subtext">Embedded or global testing endpoints</div>
            </Card>
          </div>

          {/* Controls Bar */}
          <div className="dest-controls-bar">
            <div className="dest-search-input">
              <Input
                placeholder="Search destinations by name, URL, or scope..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <div className="dest-filter-select">
              <select
                className="dest-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Enabled Only</option>
                <option value="INACTIVE">Disabled Only</option>
              </select>
            </div>
          </div>

          {/* Destinations Table */}
          <Card className="destinations-table-card">
            {isLoading ? (
              <div className="table-loading-container">
                <LoadingSpinner message="Loading configured destinations..." />
              </div>
            ) : filteredDestinations.length === 0 ? (
              <EmptyState
                title="No Destinations Found"
                description={
                  searchQuery || statusFilter !== 'ALL'
                    ? 'No destinations match your search or filter criteria.'
                    : 'No destinations have been configured yet. Create a new destination or start with the Local Mock Server.'
                }
                actionLabel="Create Destination"
                onAction={handleOpenCreateModal}
              />
            ) : (
              <div className="table-responsive">
                <table className="destinations-table">
                  <thead>
                    <tr>
                      <th>Destination Name</th>
                      <th>Base URL & Endpoint</th>
                      <th>Version</th>
                      <th>Authentication</th>
                      <th>Scope</th>
                      <th>Status</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredDestinations.map((dest) => (
                      <React.Fragment key={dest.id}>
                        <tr>
                        <td>
                          <div className="dest-name-cell">
                            <span className="dest-name-title">{dest.name}</span>
                            {dest.mock && (
                              <span className="mock-badge" title="Embedded Mock Server for local testing">
                                Embedded Mock
                              </span>
                            )}
                          </div>
                        </td>
                        <td>
                          <code className="dest-url-code" title={dest.baseUrl}>
                            {dest.baseUrl}
                          </code>
                        </td>
                        <td>
                          <span className="version-pill">{dest.fhirVersion || 'R4'}</span>
                        </td>
                        <td>
                          <div className="auth-type-cell">
                            <span className={`auth-pill ${dest.authType === 'BEARER_TOKEN' ? 'auth-bearer' : 'auth-none'}`}>
                              {dest.authType === 'BEARER_TOKEN' ? 'Bearer Token' : 'None / Public'}
                            </span>
                            {dest.hasAuthToken && (
                              <span className="token-present-badge" title="Secret token safely encrypted (AES-256-GCM)">
                                🔒 Token Set
                              </span>
                            )}
                          </div>
                        </td>
                        <td>
                          {dest.platformWide ? (
                            <span className="scope-pill scope-platform">Platform-wide</span>
                          ) : (
                            <span className="scope-pill scope-hospital" title={dest.hospitalName}>
                              {dest.hospitalName || 'Hospital-specific'}
                            </span>
                          )}
                        </td>
                        <td>
                          <StatusBadge status={dest.enabled ? 'ACTIVE' : 'INACTIVE'} />
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div className="table-actions-group">
                            {/* Test Connection: probes {baseUrl}/metadata — no patient data sent */}
                            <button
                              type="button"
                              className="action-icon-btn"
                              title="Test Connection (probes /metadata — no patient data sent)"
                              disabled={testingDestinationId === dest.id}
                              onClick={() => handleTestConnection(dest)}
                            >
                              {testingDestinationId === dest.id ? (
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ animation: 'spin 1s linear infinite' }}>
                                  <circle cx="12" cy="12" r="10" strokeDasharray="40" strokeDashoffset="10" />
                                </svg>
                              ) : (
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                                </svg>
                              )}
                            </button>
                            <button
                              type="button"
                              className="action-icon-btn"
                              title="View Deliveries"
                              onClick={() => handleViewDeliveries(dest)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10" />
                                <polyline points="12 6 12 12 16 14" />
                              </svg>
                            </button>
                            <button
                              type="button"
                              className="action-icon-btn"
                              title="Edit Destination"
                              onClick={() => handleOpenEditModal(dest)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                              </svg>
                            </button>
                            <button
                              type="button"
                              className={`action-icon-btn ${dest.enabled ? 'text-warning' : 'text-success'}`}
                              title={dest.enabled ? 'Disable Destination' : 'Enable Destination'}
                              onClick={() => handleToggleStatus(dest)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                {dest.enabled ? (
                                  <circle cx="12" cy="12" r="10" />
                                ) : (
                                  <polygon points="5 3 19 12 5 21 5 3" />
                                )}
                              </svg>
                            </button>
                            <button
                              type="button"
                              className="action-icon-btn text-danger"
                              title="Delete Destination"
                              onClick={() => handleDeleteDestination(dest)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <polyline points="3 6 5 6 21 6" />
                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                              </svg>
                            </button>
                          </div>
                        </td>
                      </tr>
                      {/* Inline connection test result row */}
                      {testResults[dest.id] && (
                        <tr key={`${dest.id}-test-result`} className="test-result-row">
                          <td colSpan={7}>
                            <div className={`connection-test-result ${testResults[dest.id].success ? 'test-success' : 'test-failure'}`}>
                              <span className="test-result-icon">
                                {testResults[dest.id].success ? '✅' : '❌'}
                              </span>
                              <span className="test-result-message">{testResults[dest.id].message}</span>
                              {testResults[dest.id].latencyMs != null && (
                                <span className="test-result-badge">{testResults[dest.id].latencyMs} ms</span>
                              )}
                              {testResults[dest.id].httpStatusCode && (
                                <span className="test-result-badge">HTTP {testResults[dest.id].httpStatusCode}</span>
                              )}
                              {testResults[dest.id].serverFhirVersion && (
                                <span className="test-result-badge">FHIR {testResults[dest.id].serverFhirVersion}</span>
                              )}
                              {testResults[dest.id].serverSoftware && (
                                <span className="test-result-badge">{testResults[dest.id].serverSoftware}</span>
                              )}
                              <button
                                type="button"
                                className="test-result-dismiss"
                                onClick={() => setTestResults((prev) => { const n = { ...prev }; delete n[dest.id]; return n; })}
                                title="Dismiss"
                              >×</button>
                            </div>
                          </td>
                        </tr>
                        )}
                      </React.Fragment>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}

      {/* TAB 2: MOCK FHIR SERVER INSPECTOR */}
      {activeTab === 'mock-inspector' && (
        <div className="mock-inspector-container">
          <Card className="mock-banner-card">
            <div className="mock-banner-content">
              <div>
                <h2 className="mock-banner-title">Embedded Mock FHIR R4 Server</h2>
                <p className="mock-banner-desc">
                  Accepts FHIR resources at <code>/api/mock-fhir/Bundle</code>, <code>/api/mock-fhir/Patient</code>, etc., returns HTTP 201 Created, and buffers recent resources in memory (up to 100). No external ports or services required.
                </p>
              </div>
              <div className="mock-banner-actions">
                <Button variant="secondary" onClick={loadMockMessages} disabled={isMockLoading}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="btn-icon">
                    <path d="M23 4v6h-6" />
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                  </svg>
                  Refresh Feed
                </Button>
                <Button variant="danger" onClick={handleClearMockMessages} disabled={mockMessages.length === 0}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="btn-icon">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                  Clear Buffer
                </Button>
              </div>
            </div>
          </Card>

          <Card className="mock-messages-card">
            {isMockLoading ? (
              <div className="table-loading-container">
                <LoadingSpinner message="Fetching mock server payload buffer..." />
              </div>
            ) : mockMessages.length === 0 ? (
              <EmptyState
                title="Mock Buffer Empty"
                description="No FHIR resources have been sent to the embedded mock server yet. Go to the Convert page or History page to send a converted FHIR bundle!"
              />
            ) : (
              <div className="mock-messages-list">
                {mockMessages.map((msg, index) => (
                  <div key={msg.id || index} className="mock-message-item">
                    <div className="mock-msg-header">
                      <div className="mock-msg-left">
                        <span className="mock-type-pill">{msg.resourceType}</span>
                        <span className="mock-status-pill">HTTP 201 Created</span>
                        <span className="mock-tracking-id">ID: {msg.id}</span>
                      </div>
                      <div className="mock-msg-time">
                        {new Date(msg.receivedAt).toLocaleTimeString()} · {new Date(msg.receivedAt).toLocaleDateString()}
                      </div>
                    </div>
                    <div className="mock-msg-body">
                      <div className="mock-summary-text">{msg.summary}</div>
                      {/* Backend serializes field as 'hasAuthHeader' (boolean) — never expose raw header value */}
                      {msg.hasAuthHeader && (
                        <div className="mock-auth-info">
                          <span>🔒 Auth Header:</span> <code>Bearer ••••••••</code>
                        </div>
                      )}
                    </div>
                    <div className="mock-msg-footer">
                      <Button
                        variant="secondary"
                        size="small"
                        onClick={() => setViewingMockResource(msg)}
                      >
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="btn-icon">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                        Inspect Raw Payload
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}

      {/* CREATE / EDIT MODAL */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={() => !isSaving && setIsModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {editingDestinationId ? 'Edit Destination' : 'Configure New FHIR Destination'}
              </h2>
              <button
                className="modal-close-btn"
                onClick={() => !isSaving && setIsModalOpen(false)}
                disabled={isSaving}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSaveDestination} className="modal-form">
              {/* Quick Fill Preset Button for New Destination */}
              {!editingDestinationId && (
                <div className="quick-fill-box">
                  <div className="quick-fill-info">
                    <strong>Testing locally?</strong> Use the embedded Mock FHIR Server with zero configuration.
                  </div>
                  <button
                    type="button"
                    className="quick-fill-btn"
                    onClick={handleFillMockServerDefaults}
                  >
                    ⚡ Fill Local Mock Preset
                  </button>
                </div>
              )}

              {formErrors.submit && (
                <div className="form-submit-error">{formErrors.submit}</div>
              )}

              <Input
                label="Destination Name *"
                placeholder="e.g., Regional HIE Endpoint or Local Mock Server"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                error={formErrors.name}
                disabled={isSaving}
              />

              <Input
                label="Base URL *"
                placeholder="e.g., https://fhir.hospital.org/r4 or http://localhost:8080/api/mock-fhir"
                value={formData.baseUrl}
                onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
                error={formErrors.baseUrl}
                disabled={isSaving}
                helpText="Only HTTP/HTTPS URLs allowed. Private network URLs are rejected by SSRF filters, except for the internal mock server."
              />

              <div className="form-row-2col">
                <div className="form-group">
                  <label className="form-label">FHIR Version</label>
                  <select
                    className="form-select"
                    value={formData.fhirVersion}
                    onChange={(e) => setFormData({ ...formData, fhirVersion: e.target.value })}
                    disabled={isSaving}
                  >
                    <option value="R4">FHIR R4 (Standard)</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Authentication Type</label>
                  <select
                    className="form-select"
                    value={formData.authType}
                    onChange={(e) => setFormData({ ...formData, authType: e.target.value })}
                    disabled={isSaving}
                  >
                    <option value="NONE">None / Open Endpoint</option>
                    <option value="BEARER_TOKEN">Bearer Token</option>
                  </select>
                </div>
              </div>

              {formData.authType === 'BEARER_TOKEN' && (
                <div className="token-field-container">
                  <div className="token-field-header">
                    <label className="form-label">
                      Bearer Authentication Token {editingDestinationId ? '(Optional on Update)' : '*'}
                    </label>
                    <button
                      type="button"
                      className="token-toggle-btn"
                      onClick={() => setShowTokenInput(!showTokenInput)}
                    >
                      {showTokenInput ? 'Hide' : 'Show'}
                    </button>
                  </div>
                  <Input
                    type={showTokenInput ? 'text' : 'password'}
                    placeholder={
                      editingDestinationId
                        ? 'Leave empty to retain existing encrypted secret'
                        : 'Enter bearer token (e.g., eyJhbGciOi...)'
                    }
                    value={formData.authToken}
                    onChange={(e) => setFormData({ ...formData, authToken: e.target.value })}
                    error={formErrors.authToken}
                    disabled={isSaving}
                  />
                  <div className="token-security-note">
                    🔒 Credentials are encrypted at rest with AES-256-GCM. Secret tokens are never exposed in UI responses.
                  </div>
                </div>
              )}

              <div className="checkbox-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.enabled}
                    onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                    disabled={isSaving}
                  />
                  <span>Enable this destination immediately</span>
                </label>
              </div>

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setIsModalOpen(false)}
                  disabled={isSaving}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={isSaving}>
                  {isSaving ? 'Saving...' : editingDestinationId ? 'Update Destination' : 'Create Destination'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELIVERIES AUDIT MODAL */}
      {deliveriesModalOpen && (
        <div className="modal-backdrop" onClick={() => setDeliveriesModalOpen(false)}>
          <div className="modal-card modal-card-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h2 className="modal-title">Delivery History</h2>
                <div className="modal-subtitle">
                  Destination: <strong>{selectedDestinationForDeliveries?.name}</strong>
                </div>
              </div>
              <button className="modal-close-btn" onClick={() => setDeliveriesModalOpen(false)}>
                &times;
              </button>
            </div>

            <div className="modal-body-scrollable">
              {isDeliveriesLoading ? (
                <div className="table-loading-container">
                  <LoadingSpinner message="Loading delivery log..." />
                </div>
              ) : destinationDeliveries.length === 0 ? (
                <EmptyState
                  title="No Deliveries Yet"
                  description="No conversion outputs have been dispatched to this destination."
                />
              ) : (
                <table className="deliveries-table">
                  <thead>
                    <tr>
                      <th>Timestamp</th>
                      <th>Conversion ID</th>
                      <th>Status</th>
                      <th>HTTP Code</th>
                      <th>Details / Errors</th>
                    </tr>
                  </thead>
                  <tbody>
                    {destinationDeliveries.map((del) => (
                      <tr key={del.id}>
                        <td>{new Date(del.attemptTimestamp).toLocaleString()}</td>
                        <td>
                          <code>#{del.conversionId}</code>
                        </td>
                        <td>
                          <StatusBadge status={del.status} />
                        </td>
                        <td>
                          <span className={`http-code-badge http-${del.httpStatusCode || 'err'}`}>
                            {del.httpStatusCode || 'N/A'}
                          </span>
                        </td>
                        <td>
                          <span className="delivery-error-text">
                            {del.errorMessage || 'Delivered successfully without errors.'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="modal-footer">
              <Button variant="secondary" onClick={() => setDeliveriesModalOpen(false)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* RAW MOCK PAYLOAD INSPECTION MODAL */}
      {viewingMockResource && (
        <div className="modal-backdrop" onClick={() => setViewingMockResource(null)}>
          <div className="modal-card modal-card-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h2 className="modal-title">
                  Mock Resource Payload: {viewingMockResource.resourceType}
                </h2>
                <div className="modal-subtitle">
                  Tracking ID: <code>{viewingMockResource.id}</code> · Received at {new Date(viewingMockResource.receivedAt).toLocaleString()}
                </div>
              </div>
              <button className="modal-close-btn" onClick={() => setViewingMockResource(null)}>
                &times;
              </button>
            </div>

            <div className="modal-body-code">
              <pre className="code-viewer-pre">
                <code>
                  {(() => {
                    try {
                      return JSON.stringify(JSON.parse(viewingMockResource.rawPayload), null, 2);
                    } catch {
                      return viewingMockResource.rawPayload;
                    }
                  })()}
                </code>
              </pre>
            </div>

            <div className="modal-footer">
              <Button
                variant="secondary"
                onClick={() => {
                  navigator.clipboard.writeText(viewingMockResource.rawPayload);
                  alert('FHIR JSON payload copied to clipboard!');
                }}
              >
                📋 Copy JSON
              </Button>
              <Button variant="primary" onClick={() => setViewingMockResource(null)}>
                Done
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
