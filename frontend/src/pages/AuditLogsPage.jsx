import React, { useState, useEffect, useCallback } from 'react';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import Button from '../components/Button';
import Input from '../components/Input';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import auditService from '../services/auditService';
import './AuditLogsPage.css';

const ACTION_OPTIONS = [
  { label: 'All Actions', value: 'ALL' },
  { label: 'Login Success', value: 'LOGIN_SUCCESS' },
  { label: 'Login Failure', value: 'LOGIN_FAILURE' },
  { label: 'HL7 Conversion Success', value: 'HL7_CONVERSION_SUCCESS' },
  { label: 'FHIR Delivery Success', value: 'FHIR_DELIVERY_SUCCESS' },
  { label: 'FHIR Delivery Failure', value: 'FHIR_DELIVERY_FAILURE' },
  { label: 'Retry Scheduled', value: 'RETRY_SCHEDULED' },
  { label: 'Retry Attempt', value: 'RETRY_ATTEMPT' },
  { label: 'Retry Success', value: 'RETRY_SUCCESS' },
  { label: 'Retry Failure', value: 'RETRY_FAILURE' },
  { label: 'Manual Retry Requested', value: 'MANUAL_RETRY_REQUESTED' },
  { label: 'Destination Create', value: 'DESTINATION_CREATE' },
  { label: 'Destination Update', value: 'DESTINATION_UPDATE' },
  { label: 'Destination Toggle Status', value: 'DESTINATION_STATUS_TOGGLE' },
  { label: 'Destination Delete', value: 'DESTINATION_DELETE' },
  { label: 'User Create', value: 'USER_CREATE' },
  { label: 'User Update', value: 'USER_UPDATE' },
  { label: 'Hospital Create', value: 'HOSPITAL_CREATE' },
  { label: 'Hospital Update', value: 'HOSPITAL_UPDATE' },
  { label: 'Access Denied', value: 'ACCESS_DENIED' },
];

const RESOURCE_TYPES = [
  { label: 'All Resources', value: 'ALL' },
  { label: 'Security / Auth', value: 'SECURITY' },
  { label: 'Conversion', value: 'CONVERSION' },
  { label: 'FHIR Delivery', value: 'FHIR_DELIVERY' },
  { label: 'Destination', value: 'DESTINATION' },
  { label: 'User', value: 'USER' },
  { label: 'Hospital', value: 'HOSPITAL' },
];

export default function AuditLogsPage() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [actionFilter, setActionFilter] = useState('ALL');
  const [outcomeFilter, setOutcomeFilter] = useState('ALL');
  const [resourceTypeFilter, setResourceTypeFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedLog, setSelectedLog] = useState(null);

  const fetchLogs = useCallback(async (targetPage = page) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await auditService.getAuditLogs({
        page: targetPage,
        size,
        action: actionFilter !== 'ALL' ? actionFilter : undefined,
        outcome: outcomeFilter !== 'ALL' ? outcomeFilter : undefined,
        resourceType: resourceTypeFilter !== 'ALL' ? resourceTypeFilter : undefined,
        search: searchQuery.trim() || undefined,
        startDate: startDate ? new Date(startDate).toISOString() : undefined,
        endDate: endDate ? new Date(endDate).toISOString() : undefined,
      });

      setLogs(response.content || []);
      setPage(response.page ?? targetPage);
      setTotalPages(response.totalPages ?? 0);
      setTotalElements(response.totalElements ?? 0);
    } catch (err) {
      setError(err.userMessage || err.message || 'Failed to retrieve system audit records.');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, actionFilter, outcomeFilter, resourceTypeFilter, searchQuery, startDate, endDate]);

  useEffect(() => {
    let ignore = false;
    async function fetchFilteredLogs() {
      try {
        const response = await auditService.getAuditLogs({
          page: 0,
          size,
          action: actionFilter !== 'ALL' ? actionFilter : undefined,
          outcome: outcomeFilter !== 'ALL' ? outcomeFilter : undefined,
          resourceType: resourceTypeFilter !== 'ALL' ? resourceTypeFilter : undefined,
          search: searchQuery.trim() || undefined,
          startDate: startDate ? new Date(startDate).toISOString() : undefined,
          endDate: endDate ? new Date(endDate).toISOString() : undefined,
        });
        if (ignore) return;
        setLogs(response.content || []);
        setPage(0);
        setTotalPages(response.totalPages ?? 0);
        setTotalElements(response.totalElements ?? 0);
      } catch (err) {
        if (!ignore) {
          setError(err.userMessage || err.message || 'Failed to retrieve system audit records.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }
    fetchFilteredLogs();
    return () => {
      ignore = true;
    };
  }, [actionFilter, outcomeFilter, resourceTypeFilter, size, searchQuery, startDate, endDate]);

  const handleSearchSubmit = (e) => {
    if (e) e.preventDefault();
    fetchLogs(0);
  };

  const handleResetFilters = () => {
    setActionFilter('ALL');
    setOutcomeFilter('ALL');
    setResourceTypeFilter('ALL');
    setSearchQuery('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const getActionBadgeClass = (action) => {
    if (!action) return 'action-default';
    if (action.startsWith('LOGIN_')) return 'action-auth';
    if (action.startsWith('HL7_')) return 'action-conversion';
    if (action.startsWith('FHIR_') || action.startsWith('RETRY_') || action.startsWith('MANUAL_RETRY')) return 'action-delivery';
    if (action.startsWith('DESTINATION_')) return 'action-destination';
    if (action === 'ACCESS_DENIED') return 'action-security';
    if (action.startsWith('USER_') || action.startsWith('HOSPITAL_')) return 'action-admin';
    return 'action-default';
  };

  const formatTimestamp = (ts) => {
    if (!ts) return '—';
    try {
      const d = new Date(ts);
      return isNaN(d.getTime()) ? ts : d.toLocaleString();
    } catch {
      return ts;
    }
  };

  return (
    <div className="audit-page">
      {/* Header with Title and Refresh */}
      <div className="audit-intro-header">
        <div className="audit-intro">
          <h2 className="audit-title">System Audit Log</h2>
          <p className="audit-subtitle">
            Immutable audit trail of authentication events, HL7-to-FHIR conversions, destination modifications, and clinical dispatches.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchLogs(page)}
          disabled={isLoading}
          title="Reload audit records from server"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            style={{ width: 14, height: 14, marginRight: 6 }}
          >
            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Refresh Logs
        </Button>
      </div>

      {error && (
        <ErrorMessage
          title="Audit Log Error"
          message={error}
          onDismiss={() => setError(null)}
        />
      )}

      {/* Filter and Query Card */}
      <Card className="audit-filter-card">
        <form onSubmit={handleSearchSubmit}>
          <div className="audit-filters-grid">
            {/* Search Input */}
            <div className="audit-filter-group" style={{ gridColumn: 'span 2' }}>
              <label htmlFor="auditSearch" className="audit-filter-label">Search</label>
              <Input
                id="auditSearch"
                placeholder="Search username, resource ID, or details..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                disabled={isLoading}
              />
            </div>

            {/* Action Filter */}
            <div className="audit-filter-group">
              <label htmlFor="auditAction" className="audit-filter-label">Event Action</label>
              <select
                id="auditAction"
                className="audit-filter-select"
                value={actionFilter}
                onChange={(e) => setActionFilter(e.target.value)}
                disabled={isLoading}
              >
                {ACTION_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Outcome Filter */}
            <div className="audit-filter-group">
              <label className="audit-filter-label">Outcome</label>
              <div className="audit-outcome-pills">
                {['ALL', 'SUCCESS', 'FAILURE'].map((out) => (
                  <button
                    key={out}
                    type="button"
                    className={`audit-outcome-pill ${outcomeFilter === out ? 'active' : ''}`}
                    onClick={() => setOutcomeFilter(out)}
                    disabled={isLoading}
                  >
                    {out}
                  </button>
                ))}
              </div>
            </div>

            {/* Resource Type */}
            <div className="audit-filter-group">
              <label htmlFor="auditResourceType" className="audit-filter-label">Resource Type</label>
              <select
                id="auditResourceType"
                className="audit-filter-select"
                value={resourceTypeFilter}
                onChange={(e) => setResourceTypeFilter(e.target.value)}
                disabled={isLoading}
              >
                {RESOURCE_TYPES.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Start Date */}
            <div className="audit-filter-group">
              <label htmlFor="auditStartDate" className="audit-filter-label">From Date</label>
              <input
                id="auditStartDate"
                type="date"
                className="audit-filter-input"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                disabled={isLoading}
              />
            </div>

            {/* End Date */}
            <div className="audit-filter-group">
              <label htmlFor="auditEndDate" className="audit-filter-label">To Date</label>
              <input
                id="auditEndDate"
                type="date"
                className="audit-filter-input"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                disabled={isLoading}
              />
            </div>

            {/* Actions */}
            <div className="audit-filter-actions">
              <Button
                variant="primary"
                size="sm"
                type="submit"
                disabled={isLoading}
              >
                Apply Filters
              </Button>
              <Button
                variant="outline"
                size="sm"
                type="button"
                onClick={handleResetFilters}
                disabled={isLoading}
              >
                Reset
              </Button>
            </div>
          </div>
        </form>
      </Card>

      {/* Audit Log Table Card */}
      <Card className="audit-table-card">
        {isLoading ? (
          <div style={{ padding: '3.5rem 1rem', textAlign: 'center' }}>
            <LoadingSpinner message="Retrieving secure audit events from database..." />
          </div>
        ) : logs.length === 0 ? (
          <EmptyState
            title="No audit events found"
            description={
              searchQuery || actionFilter !== 'ALL' || outcomeFilter !== 'ALL'
                ? 'No audit log entries match your current filter parameters.'
                : 'Security and business events will appear here as operations are performed.'
            }
            action={
              (searchQuery || actionFilter !== 'ALL' || outcomeFilter !== 'ALL') && (
                <Button variant="outline" size="sm" onClick={handleResetFilters}>
                  Clear Filters
                </Button>
              )
            }
          />
        ) : (
          <>
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Log ID</th>
                    <th>Timestamp</th>
                    <th>Actor</th>
                    <th>Tenant / Hospital</th>
                    <th>Action</th>
                    <th>Resource</th>
                    <th>Outcome</th>
                    <th>Details</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id}>
                      <td className="font-mono text-bold">#{log.id}</td>
                      <td className="text-secondary text-sm">{formatTimestamp(log.timestamp)}</td>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span className="font-mono text-bold">{log.username || 'Anonymous'}</span>
                          {log.actorId && (
                            <span className="text-secondary text-sm" style={{ fontSize: '0.7rem' }}>
                              ID: {log.actorId}
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        {log.hospitalName ? (
                          <span className="hospital-badge" title={log.hospitalName}>
                            {log.hospitalName}
                          </span>
                        ) : (
                          <span className="text-secondary text-sm">—</span>
                        )}
                      </td>
                      <td>
                        <span className={`audit-action-badge ${getActionBadgeClass(log.action)}`}>
                          {log.action}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span className="font-mono" style={{ fontSize: '0.8rem' }}>
                            {log.resourceType || '—'}
                          </span>
                          {log.resourceId && (
                            <span className="text-secondary font-mono" style={{ fontSize: '0.7rem' }}>
                              #{log.resourceId}
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <StatusBadge status={log.outcome} />
                      </td>
                      <td className="audit-details-cell" title={log.details || ''}>
                        {log.details || '—'}
                      </td>
                      <td>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => setSelectedLog(log)}
                        >
                          Inspect
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            <div className="audit-pagination-bar">
              <div className="pagination-info">
                Showing {logs.length > 0 ? page * size + 1 : 0} to{' '}
                {Math.min((page + 1) * size, totalElements)} of {totalElements} audit events
              </div>

              <div className="pagination-controls">
                <label htmlFor="auditPageSize" style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  Per page:
                </label>
                <select
                  id="auditPageSize"
                  className="pagination-size-select"
                  value={size}
                  onChange={(e) => setSize(Number(e.target.value))}
                  disabled={isLoading}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchLogs(page - 1)}
                  disabled={page <= 0 || isLoading}
                >
                  Previous
                </Button>

                <span className="pagination-pages">
                  Page {totalPages > 0 ? page + 1 : 0} of {totalPages}
                </span>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchLogs(page + 1)}
                  disabled={page + 1 >= totalPages || isLoading}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      {/* Audit Record Inspection Modal */}
      {selectedLog && (
        <div className="modal-backdrop" onClick={() => setSelectedLog(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-titles">
                <div className="modal-badge-line">
                  <StatusBadge status={selectedLog.outcome} />
                  <span className="modal-rec-id">Audit Entry #{selectedLog.id}</span>
                </div>
                <h3 className="modal-title">
                  {selectedLog.action}
                </h3>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setSelectedLog(null)}
                aria-label="Close modal"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <div className="modal-body">
              {/* Event Attributes Grid */}
              <div className="modal-attributes-grid">
                <div className="attr-item">
                  <span className="attr-label">Timestamp:</span>
                  <span className="attr-value">{formatTimestamp(selectedLog.timestamp)}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Outcome:</span>
                  <span className="attr-value"><StatusBadge status={selectedLog.outcome} /></span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Actor Username:</span>
                  <span className="attr-value font-mono">{selectedLog.username || 'Anonymous'}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Actor User ID:</span>
                  <span className="attr-value font-mono">{selectedLog.actorId ? `#${selectedLog.actorId}` : 'N/A'}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Tenant / Hospital:</span>
                  <span className="attr-value font-mono">
                    {selectedLog.hospitalName
                      ? `${selectedLog.hospitalName} (ID: ${selectedLog.hospitalId})`
                      : 'Platform-level'}
                  </span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Resource Type & ID:</span>
                  <span className="attr-value font-mono">
                    {selectedLog.resourceType} {selectedLog.resourceId ? `(#${selectedLog.resourceId})` : ''}
                  </span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Correlation ID:</span>
                  <span className="attr-value font-mono">{selectedLog.correlationId || 'N/A'}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">ISO Timestamp:</span>
                  <span className="attr-value font-mono text-sm">{selectedLog.timestamp}</span>
                </div>
              </div>

              {/* Safe Details Box */}
              <div className="modal-section" style={{ marginTop: '1.25rem' }}>
                <h4 className="modal-section-title">Audit Record Details</h4>
                <div className="audit-modal-details-box">
                  {selectedLog.details || 'No additional details recorded.'}
                </div>
              </div>

              {/* Privacy Notice */}
              <div className="audit-modal-privacy-note">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: 18, height: 18, flexShrink: 0 }}>
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
                <span>
                  <strong>Privacy & Security Guaranteed:</strong> Passwords, JWT tokens, and sensitive clinical payloads are never recorded in audit trail storage.
                </span>
              </div>
            </div>

            <div className="modal-footer">
              <Button variant="secondary" size="md" onClick={() => setSelectedLog(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
