import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import Button from '../components/Button';
import { useAuth } from '../context/useAuth';
import dashboardService from '../services/dashboardService';
import './DashboardPage.css';

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    totalConversions: 0,
    successfulConversions: 0,
    failedConversions: 0,
    conversionSuccessRate: 0.0,
    patientsProcessed: 0,
    encountersProcessed: 0,
    observationsProcessed: 0,
    totalDeliveries: 0,
    successfulDeliveries: 0,
    failedDeliveries: 0,
    retryingDeliveries: 0,
    pendingDeliveries: 0,
    deliverySuccessRate: 0.0,
    totalDestinations: 0,
    enabledDestinations: 0,
    engineStatus: 'UP',
    databaseStatus: 'UP',
    recentConversions: [],
    recentDeliveries: [],
    hospitalName: null,
    hospitalCode: null,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [dashboardError, setDashboardError] = useState('');

  const loadStats = useCallback(async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setIsRefreshing(true);
      setDashboardError('');
    } else {
      setIsLoading(true);
      setDashboardError('');
    }
    try {
      const [statsResult, healthResult] = await Promise.allSettled([
        dashboardService.getStats(),
        dashboardService.getHealth(),
      ]);

      if (statsResult.status === 'fulfilled' && statsResult.value) {
        const data = { ...statsResult.value };
        if (healthResult.status === 'fulfilled' && healthResult.value) {
          data.engineStatus = healthResult.value.engine || healthResult.value.status || data.engineStatus;
          data.databaseStatus = healthResult.value.database || data.databaseStatus;
        } else if (healthResult.status === 'rejected') {
          data.engineStatus = 'DEGRADED';
          data.databaseStatus = 'DOWN';
        }
        setStats(data);
      } else if (statsResult.status === 'rejected') {
        const err = statsResult.reason;
        console.error('Failed to load dashboard metrics:', err);
        setDashboardError(err?.userMessage || err?.message || 'Failed to load dashboard metrics from server.');
      }
    } catch (err) {
      console.error('Dashboard error:', err);
      setDashboardError(err?.userMessage || err?.message || 'Failed to load dashboard metrics.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    async function fetchInitialStats() {
      try {
        const [statsResult, healthResult] = await Promise.allSettled([
          dashboardService.getStats(),
          dashboardService.getHealth(),
        ]);
        if (ignore) return;

        if (statsResult.status === 'fulfilled' && statsResult.value) {
          const data = { ...statsResult.value };
          if (healthResult.status === 'fulfilled' && healthResult.value) {
            data.engineStatus = healthResult.value.engine || healthResult.value.status || data.engineStatus;
            data.databaseStatus = healthResult.value.database || data.databaseStatus;
          } else if (healthResult.status === 'rejected') {
            data.engineStatus = 'DEGRADED';
            data.databaseStatus = 'DOWN';
          }
          setStats(data);
        } else if (statsResult.status === 'rejected') {
          const err = statsResult.reason;
          console.error('Failed to load dashboard metrics:', err);
          setDashboardError(err?.userMessage || err?.message || 'Failed to load dashboard metrics from server.');
        }
      } catch (err) {
        if (!ignore) {
          console.error('Dashboard error:', err);
          setDashboardError(err?.userMessage || err?.message || 'Failed to load dashboard metrics.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
          setIsRefreshing(false);
        }
      }
    }
    fetchInitialStats();
    return () => {
      ignore = true;
    };
  }, []);

  const conversionCards = [
    {
      label: 'Total Conversions',
      value: isLoading ? '...' : stats.totalConversions,
      desc: stats.totalConversions > 0 ? `${stats.conversionSuccessRate}% success rate` : 'No conversions recorded',
      color: 'blue',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
      ),
    },
    {
      label: 'Successful Conversions',
      value: isLoading ? '...' : stats.successfulConversions,
      desc: 'Mapped cleanly to FHIR R4',
      color: 'green',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
    {
      label: 'Failed Conversions',
      value: isLoading ? '...' : stats.failedConversions,
      desc: 'Validation or structural issues',
      color: 'red',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
    {
      label: 'Patients Processed',
      value: isLoading ? '...' : stats.patientsProcessed,
      desc: 'Unique patient identifiers',
      color: 'indigo',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      ),
    },
    {
      label: 'Observations Processed',
      value: isLoading ? '...' : stats.observationsProcessed,
      desc: 'Lab and clinical metrics',
      color: 'amber',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
      ),
    },
  ];

  const deliveryCards = [
    {
      label: 'Total FHIR Deliveries',
      value: isLoading ? '...' : stats.totalDeliveries,
      desc: stats.totalDeliveries > 0 ? `${stats.deliverySuccessRate}% delivery rate` : 'No delivery attempts',
      color: 'cyan',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
      ),
    },
    {
      label: 'Successful Deliveries',
      value: isLoading ? '...' : stats.successfulDeliveries,
      desc: 'Accepted by destination servers',
      color: 'green',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M5 13l4 4L19 7" />
        </svg>
      ),
    },
    {
      label: 'Failed Deliveries',
      value: isLoading ? '...' : stats.failedDeliveries,
      desc: 'Exhausted retries or rejected',
      color: 'red',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M6 18L18 6M6 6l12 12" />
        </svg>
      ),
    },
    {
      label: 'In-Flight / Retrying',
      value: isLoading ? '...' : (stats.retryingDeliveries || 0) + (stats.pendingDeliveries || 0),
      desc: `${stats.retryingDeliveries || 0} retrying, ${stats.pendingDeliveries || 0} pending`,
      color: 'amber',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
    {
      label: 'FHIR Destinations',
      value: isLoading ? '...' : `${stats.enabledDestinations || 0} / ${stats.totalDestinations || 0}`,
      desc: 'Active / Configured endpoints',
      color: 'purple',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
      ),
    },
  ];

  const recentConversions = stats.recentConversions || [];
  const recentDeliveries = stats.recentDeliveries || [];

  return (
    <div className="dashboard-page">
      {/* Welcome Banner */}
      <div className="dashboard-banner">
        <div className="banner-content">
          <h2 className="banner-title">
            Clinical Interoperability Dashboard
            {stats.hospitalName && (
              <span className="tenant-badge-text">
                — {stats.hospitalName} ({stats.hospitalCode})
              </span>
            )}
          </h2>
          <p className="banner-subtitle">
            {stats.hospitalName
              ? `Real-time tenant metrics strictly isolated to ${stats.hospitalName}.`
              : 'Real-time pipeline monitoring and statistics for HL7 v2 to FHIR R4 transformations.'}
          </p>
        </div>
        <div className="banner-actions">
          <button
            type="button"
            className="btn btn-secondary refresh-btn"
            onClick={() => loadStats(true)}
            disabled={isLoading || isRefreshing}
            title="Refresh dashboard statistics"
          >
            <svg
              className={`refresh-icon ${isRefreshing ? 'spin' : ''}`}
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>{isRefreshing ? 'Refreshing...' : 'Refresh'}</span>
          </button>
          {user?.role === 'ADMIN' && (
            <Link to="/users/create">
              <Button variant="secondary" size="md">
                Register User
              </Button>
            </Link>
          )}
          <Link to="/convert">
            <Button variant="primary" size="md">
              Start New Conversion
            </Button>
          </Link>
        </div>
      </div>

      {dashboardError && (
        <div className="dashboard-error-banner">
          <svg className="error-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div className="error-banner-text">
            <div className="error-banner-title">Dashboard Statistics Unavailable</div>
            <div className="error-banner-detail">{dashboardError}</div>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => loadStats(false)}
          >
            Retry
          </button>
        </div>
      )}

      {/* Conversion KPIs */}
      <div className="kpi-section">
        <h3 className="section-title">Conversion Pipeline Metrics</h3>
        <div className="stats-grid">
          {conversionCards.map((card, idx) => (
            <div key={idx} className={`stat-card stat-${card.color}`}>
              <div className="stat-card-header">
                <span className="stat-label">{card.label}</span>
                <div className="stat-icon-wrapper">{card.icon}</div>
              </div>
              <div className="stat-value">{card.value}</div>
              <div className="stat-desc">{card.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Delivery & Destination KPIs */}
      <div className="kpi-section">
        <h3 className="section-title">FHIR Delivery & Integration Metrics</h3>
        <div className="stats-grid">
          {deliveryCards.map((card, idx) => (
            <div key={idx} className={`stat-card stat-${card.color}`}>
              <div className="stat-card-header">
                <span className="stat-label">{card.label}</span>
                <div className="stat-icon-wrapper">{card.icon}</div>
              </div>
              <div className="stat-value">{card.value}</div>
              <div className="stat-desc">{card.desc}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="dashboard-columns">
        <div className="dashboard-main-column">
          {/* Recent Conversions Table */}
          <Card
            title="Recent Conversions"
            subtitle={
              stats.hospitalName
                ? `Audit records for ${stats.hospitalName}`
                : 'Audit records of recently processed HL7 v2 messages'
            }
            actions={
              <Link to="/history" className="view-all-link">
                View All History →
              </Link>
            }
            className="recent-conversions-card"
          >
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Control ID</th>
                    <th>Patient ID</th>
                    <th>Message Type</th>
                    <th>Status</th>
                    <th>Timestamp</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {recentConversions.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="table-empty-message">
                        No conversion records found for this tenant. Start a new conversion above!
                      </td>
                    </tr>
                  ) : (
                    recentConversions.map((rec) => (
                      <tr key={rec.id}>
                        <td className="font-mono text-bold">{rec.messageControlId || '—'}</td>
                        <td className="font-mono">{rec.patientIdentifier || '—'}</td>
                        <td>
                          <span className="type-badge">{rec.messageType || 'ADT^A01'}</span>
                        </td>
                        <td>
                          <StatusBadge status={rec.status} />
                        </td>
                        <td className="text-secondary text-sm">
                          {rec.createdAt ? new Date(rec.createdAt).toLocaleString() : '—'}
                        </td>
                        <td>
                          <Link to="/history" className="table-action-link">
                            Details
                          </Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </Card>

          {/* Recent Deliveries Table */}
          <Card
            title="Recent FHIR Deliveries"
            subtitle="Outbound transmission results to configured FHIR destinations"
            actions={
              <Link to="/destinations" className="view-all-link">
                Manage Destinations →
              </Link>
            }
            className="recent-deliveries-card"
          >
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Destination</th>
                    <th>Status</th>
                    <th>HTTP Code</th>
                    <th>Attempts</th>
                    <th>Timestamp</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {recentDeliveries.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="table-empty-message">
                        No delivery records found for this tenant. Deliver a conversion from the History page!
                      </td>
                    </tr>
                  ) : (
                    recentDeliveries.map((del) => (
                      <tr key={del.id}>
                        <td className="text-bold">{del.destinationName || 'Destination #' + del.destinationId}</td>
                        <td>
                          <StatusBadge status={del.status} />
                        </td>
                        <td className="font-mono text-sm">
                          {del.responseStatusCode ? (
                            <span className={del.responseStatusCode < 300 ? 'status-code-ok' : 'status-code-err'}>
                              {del.responseStatusCode}
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td className="text-sm font-mono">
                          {del.attemptCount || 1} / {del.maxAttempts || 3}
                        </td>
                        <td className="text-secondary text-sm">
                          {del.attemptTimestamp ? new Date(del.attemptTimestamp).toLocaleString() : '—'}
                        </td>
                        <td>
                          <Link to="/history" className="table-action-link">
                            View in History
                          </Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        {/* Engine Specification Info */}
        <div className="dashboard-side-column">
          <Card
            title="Engine Health & Specs"
            subtitle="Operational status & compliance standards"
            className="engine-specs-card"
          >
            <div className="specs-list">
              <div className="spec-item">
                <span className="spec-label">Integration Engine:</span>
                <StatusBadge status={stats.engineStatus === 'UP' ? 'OPERATIONAL' : (stats.engineStatus || 'OPERATIONAL')} />
              </div>
              <div className="spec-item">
                <span className="spec-label">Database Connectivity:</span>
                <StatusBadge status={stats.databaseStatus || 'UP'} />
              </div>
              <div className="spec-item">
                <span className="spec-label">Conversion Rate:</span>
                <span className="spec-value">
                  {stats.totalConversions > 0 ? `${stats.conversionSuccessRate}%` : 'N/A (0 msgs)'}
                </span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Delivery Rate:</span>
                <span className="spec-value">
                  {stats.totalDeliveries > 0 ? `${stats.deliverySuccessRate}%` : 'N/A (0 sent)'}
                </span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Tenant Isolation:</span>
                <span className="spec-value spec-highlight-green">
                  Strict Multi-Tenant (HIPAA BAA)
                </span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Supported Message:</span>
                <span className="spec-value font-mono">ADT^A01 (Admit Patient)</span>
              </div>
              <div className="spec-item">
                <span className="spec-label">HL7 Version:</span>
                <span className="spec-value font-mono">v2.5 / v2.3</span>
              </div>
              <div className="spec-item">
                <span className="spec-label">FHIR Standard:</span>
                <span className="spec-value font-mono">FHIR R4 (v4.0.1)</span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Resources Generated:</span>
                <span className="spec-value">Patient, Encounter, Observation</span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Data Persistence:</span>
                <span className="spec-value">MySQL 8 / Tenant-Scoped JPA</span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
