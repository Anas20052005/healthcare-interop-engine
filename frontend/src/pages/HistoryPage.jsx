import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import Button from '../components/Button';
import Input from '../components/Input';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import FHIRViewer from '../components/FHIRViewer';
import historyService from '../services/historyService';
import destinationService from '../services/destinationService';
import './HistoryPage.css';

export default function HistoryPage() {
  const navigate = useNavigate();
  const [records, setRecords] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedRecord, setSelectedRecord] = useState(null);

  // Destination and Deliveries Modal States
  const [destinations, setDestinations] = useState([]);
  const [selectedDestinationId, setSelectedDestinationId] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [modalDeliveries, setModalDeliveries] = useState([]);
  const [isDeliveriesLoading, setIsDeliveriesLoading] = useState(false);
  const [sendSuccessMessage, setSendSuccessMessage] = useState('');
  const [sendErrorMessage, setSendErrorMessage] = useState('');
  const [retryingDeliveryId, setRetryingDeliveryId] = useState(null);

  const loadDestinations = useCallback(async () => {
    try {
      const list = await destinationService.getDestinations();
      const enabledList = (Array.isArray(list) ? list : []).filter((d) => d.enabled);
      setDestinations(enabledList);
      if (enabledList.length > 0) {
        setSelectedDestinationId((prev) => {
          if (prev) return prev;
          const mockDest = enabledList.find((d) => d.mock);
          return mockDest ? mockDest.id : enabledList[0].id;
        });
      }
    } catch (e) {
      console.error('Failed to load destinations in HistoryPage', e);
    }
  }, []);

  const loadRecordDeliveries = useCallback(async (conversionId) => {
    setIsDeliveriesLoading(true);
    try {
      const list = await destinationService.getConversionDeliveries(conversionId);
      setModalDeliveries(Array.isArray(list) ? list : []);
    } catch {
      setModalDeliveries([]);
    } finally {
      setIsDeliveriesLoading(false);
    }
  }, []);

  const handleOpenRecord = (rec) => {
    setSendSuccessMessage('');
    setSendErrorMessage('');
    setSelectedRecord(rec);
    if (rec && rec.conversionId) {
      loadRecordDeliveries(rec.conversionId);
      loadDestinations();
    }
  };

  const loadHistory = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await historyService.getHistory();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.userMessage || err.message || 'Failed to retrieve conversion audit history.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    async function fetchInitial() {
      try {
        const data = await historyService.getHistory();
        if (!ignore) {
          setRecords(Array.isArray(data) ? data : []);
          setIsLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          setError(err.userMessage || err.message || 'Failed to retrieve conversion audit history.');
          setIsLoading(false);
        }
      }
    }
    fetchInitial();
    return () => {
      ignore = true;
    };
  }, []);

  const handleSendFromHistory = async () => {
    if (!selectedRecord || !selectedRecord.conversionId || !selectedDestinationId) return;

    const targetDest = destinations.find((d) => String(d.id) === String(selectedDestinationId));
    const targetName = targetDest ? targetDest.name : `Destination #${selectedDestinationId}`;
    const confirmed = window.confirm(
      `Confirm Delivery: Send FHIR R4 resources for Conversion #${selectedRecord.conversionId} to "${targetName}"?`
    );
    if (!confirmed) return;

    setIsSending(true);
    setSendSuccessMessage('');
    setSendErrorMessage('');
    try {
      const result = await destinationService.sendConversion(selectedRecord.conversionId, selectedDestinationId);
      const summaryText = result.responseSummary ? ` (${result.responseSummary})` : '';
      if (result.status === 'RETRYING') {
        setSendSuccessMessage(`Transient issue encountered. Automatic retry scheduled for ${formatTimestamp(result.nextRetryAt)}.`);
      } else {
        setSendSuccessMessage(`Dispatched successfully to "${result.destinationName}" (HTTP ${result.httpStatusCode || 'OK'})${summaryText}`);
      }
      await loadRecordDeliveries(selectedRecord.conversionId);
    } catch (err) {
      setSendErrorMessage(err.userMessage || err.message || 'Failed to dispatch conversion.');
      await loadRecordDeliveries(selectedRecord.conversionId);
    } finally {
      setIsSending(false);
    }
  };

  const handleRetryDelivery = async (deliveryId) => {
    if (!deliveryId) return;

    setRetryingDeliveryId(deliveryId);
    setSendSuccessMessage('');
    setSendErrorMessage('');

    try {
      const result = await destinationService.retryDelivery(deliveryId);
      const summaryText = result.responseSummary ? ` (${result.responseSummary})` : '';
      if (result.status === 'SUCCESS') {
        setSendSuccessMessage(`Retry succeeded to "${result.destinationName}" (HTTP ${result.httpStatusCode || 'OK'})${summaryText}`);
      } else if (result.status === 'RETRYING') {
        setSendSuccessMessage(`Retry attempt finished. Rescheduled next attempt at ${formatTimestamp(result.nextRetryAt)}.`);
      } else {
        setSendErrorMessage(`Retry attempt failed (HTTP ${result.httpStatusCode || 'N/A'}): ${result.errorMessage || 'Destination rejected request'}`);
      }
      if (selectedRecord && selectedRecord.conversionId) {
        await loadRecordDeliveries(selectedRecord.conversionId);
      }
    } catch (err) {
      setSendErrorMessage(err.userMessage || err.message || 'Failed to retry delivery.');
      if (selectedRecord && selectedRecord.conversionId) {
        await loadRecordDeliveries(selectedRecord.conversionId);
      }
    } finally {
      setRetryingDeliveryId(null);
    }
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

  const filteredRecords = useMemo(() => {
    return records.filter((rec) => {
      const matchesStatus =
        statusFilter === 'ALL' || (rec.status && rec.status.toUpperCase() === statusFilter);

      const q = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !q ||
        (rec.messageControlId && rec.messageControlId.toLowerCase().includes(q)) ||
        (rec.patientId && rec.patientId.toLowerCase().includes(q)) ||
        (rec.encounterId && rec.encounterId.toLowerCase().includes(q)) ||
        (rec.messageType && rec.messageType.toLowerCase().includes(q)) ||
        (rec.conversionId && String(rec.conversionId).includes(q));

      return matchesStatus && matchesSearch;
    });
  }, [records, searchQuery, statusFilter]);

  return (
    <div className="history-page">
      {/* Page Header Intro with Refresh Button */}
      <div className="history-intro-header">
        <div className="history-intro">
          <h2 className="history-title">Conversion Audit History</h2>
          <p className="history-subtitle">
            Historical registry of clinical HL7 messages received, validation results, and generated FHIR resources.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={loadHistory}
          disabled={isLoading}
          className="history-refresh-btn"
          title="Reload records from server"
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
          Refresh
        </Button>
      </div>

      {error && (
        <ErrorMessage
          title="Audit History Error"
          message={error}
          onDismiss={() => setError(null)}
          className="history-error-alert"
        />
      )}

      {/* Filter Toolbar */}
      <Card className="history-filter-card">
        <div className="filter-toolbar">
          <div className="filter-search-wrap">
            <Input
              id="historySearch"
              placeholder="Search by Patient ID, Control ID, Encounter, or Conv. ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
              disabled={isLoading}
            />
          </div>

          <div className="filter-status-buttons">
            <span className="filter-label">Filter by:</span>
            {['ALL', 'SUCCESS', 'FAILED'].map((st) => (
              <button
                key={st}
                type="button"
                className={`filter-tab ${statusFilter === st ? 'active' : ''}`}
                onClick={() => setStatusFilter(st)}
                disabled={isLoading}
              >
                {st}
              </button>
            ))}
          </div>
        </div>
      </Card>

      {/* History Table / State Card */}
      <Card className="history-table-card">
        {isLoading ? (
          <div className="history-loading-container" style={{ padding: '3rem 1rem', textAlign: 'center' }}>
            <LoadingSpinner message="Retrieving conversion audit records from database..." />
          </div>
        ) : records.length === 0 ? (
          <EmptyState
            title="No conversion records found"
            description="Your completed HL7 to FHIR conversions will appear here once processed by the engine."
            action={
              <Button
                variant="primary"
                size="sm"
                onClick={() => navigate('/convert')}
              >
                Go to Converter
              </Button>
            }
          />
        ) : filteredRecords.length > 0 ? (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Conv. ID</th>
                  <th>Control ID</th>
                  <th>Message Type</th>
                  <th>Patient ID</th>
                  <th>Encounter ID</th>
                  <th>Obs Count</th>
                  <th>Status</th>
                  <th>Timestamp</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredRecords.map((rec) => (
                  <tr key={rec.conversionId}>
                    <td className="font-mono text-bold">#{rec.conversionId}</td>
                    <td className="font-mono">{rec.messageControlId || '—'}</td>
                    <td>
                      <span className="type-badge">{rec.messageType || 'ADT^A01'}</span>
                    </td>
                    <td className="font-mono">{rec.patientId || '—'}</td>
                    <td className="font-mono text-secondary">{rec.encounterId || '—'}</td>
                    <td>{rec.observationCount ?? 0}</td>
                    <td>
                      <StatusBadge status={rec.status} />
                    </td>
                    <td className="text-secondary text-sm">{formatTimestamp(rec.timestamp)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenRecord(rec)}
                        >
                          Inspect
                        </Button>
                        {rec.status === 'SUCCESS' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              handleOpenRecord(rec);
                              // Auto-focus to dispatch section
                              setTimeout(() => {
                                const el = document.querySelector('.modal-delivery-section');
                                if (el) el.scrollIntoView({ behavior: 'smooth' });
                              }, 100);
                            }}
                            title="Send FHIR R4 output to configured destination"
                          >
                            Send FHIR
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No records match your criteria"
            description="Try modifying or clearing your search filter to find the conversion audit record."
            action={
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSearchQuery('');
                  setStatusFilter('ALL');
                }}
              >
                Reset Filters
              </Button>
            }
          />
        )}
      </Card>

      {/* Record Inspection Modal */}
      {selectedRecord && (
        <div className="modal-backdrop" onClick={() => setSelectedRecord(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-titles">
                <div className="modal-badge-line">
                  <StatusBadge status={selectedRecord.status} />
                  <span className="modal-rec-id">Record #{selectedRecord.conversionId}</span>
                </div>
                <h3 className="modal-title">
                  Conversion Details — {selectedRecord.messageControlId || `#${selectedRecord.conversionId}`}
                </h3>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setSelectedRecord(null)}
                aria-label="Close modal"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <div className="modal-body">
              {/* Summary Attributes */}
              <div className="modal-attributes-grid">
                <div className="attr-item">
                  <span className="attr-label">Patient ID:</span>
                  <span className="attr-value font-mono">{selectedRecord.patientId || 'N/A'}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Encounter ID:</span>
                  <span className="attr-value font-mono">{selectedRecord.encounterId || 'N/A'}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Message Type:</span>
                  <span className="attr-value font-mono">{selectedRecord.messageType}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Observations:</span>
                  <span className="attr-value font-mono">{selectedRecord.observationCount ?? 0}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Timestamp:</span>
                  <span className="attr-value">{formatTimestamp(selectedRecord.timestamp)}</span>
                </div>
                <div className="attr-item">
                  <span className="attr-label">Status:</span>
                  <span className="attr-value"><StatusBadge status={selectedRecord.status} /></span>
                </div>
              </div>

              {selectedRecord.errorMessage && (
                <div className="modal-error-notice">
                  <strong>Failure Reason:</strong> {selectedRecord.errorMessage}
                </div>
              )}

              {/* HL7 Message Accordion (if available) */}
              {selectedRecord.hl7Message && (
                <div className="modal-section">
                  <h4 className="modal-section-title">Original HL7 v2 Message</h4>
                  <pre className="modal-hl7-pre">{selectedRecord.hl7Message}</pre>
                </div>
              )}

              {/* FHIR Bundle Viewer (if available) */}
              {(selectedRecord.fhirBundle || selectedRecord.fhirOutput) && (
                <div className="modal-section">
                  <h4 className="modal-section-title">Generated FHIR R4 Output</h4>
                  <FHIRViewer fhirData={selectedRecord.fhirBundle || selectedRecord.fhirOutput} />
                </div>
              )}

              {/* Priority 5: Destination Dispatch & Delivery Audit */}
              <div className="modal-section modal-delivery-section">
                <h4 className="modal-section-title">Destination Dispatch & Delivery Audit</h4>

                {sendErrorMessage && (
                  <ErrorMessage
                    title="Dispatch Error"
                    message={sendErrorMessage}
                    onDismiss={() => setSendErrorMessage('')}
                  />
                )}
                {sendSuccessMessage && (
                  <div className="modal-send-success">
                    ✓ {sendSuccessMessage}
                  </div>
                )}

                {selectedRecord.status === 'SUCCESS' && (
                  <div className="history-dispatch-bar">
                    <div className="history-dispatch-select-wrap">
                      <label className="history-dispatch-label">Send to Destination</label>
                      <select
                        className="history-dispatch-select"
                        value={selectedDestinationId}
                        onChange={(e) => setSelectedDestinationId(e.target.value)}
                        disabled={isSending || destinations.length === 0}
                      >
                        {destinations.length === 0 ? (
                          <option value="">No active destinations configured</option>
                        ) : (
                          destinations.map((d) => (
                            <option key={d.id} value={d.id}>
                              {d.name} ({d.baseUrl}) {d.mock ? '[Mock]' : ''}
                            </option>
                          ))
                        )}
                      </select>
                    </div>

                    <Button
                      variant="primary"
                      size="sm"
                      onClick={handleSendFromHistory}
                      isLoading={isSending}
                      disabled={!selectedDestinationId || destinations.length === 0}
                    >
                      Dispatch Now
                    </Button>
                  </div>
                )}

                {/* Delivery Attempts List */}
                <div className="history-deliveries-container">
                  <div className="history-deliveries-header">
                    <span>Delivery Attempts ({modalDeliveries.length})</span>
                    <button
                      type="button"
                      className="btn-refresh-deliveries"
                      onClick={() => loadRecordDeliveries(selectedRecord.conversionId)}
                      disabled={isDeliveriesLoading}
                    >
                      ↻ Refresh
                    </button>
                  </div>

                  {isDeliveriesLoading ? (
                    <div className="history-deliveries-loading">Loading delivery attempts...</div>
                  ) : modalDeliveries.length === 0 ? (
                    <div className="history-deliveries-empty">
                      No delivery attempts recorded for this conversion record.
                    </div>
                  ) : (
                    <div className="history-deliveries-list">
                      {modalDeliveries.map((del) => (
                        <div key={del.id} className="history-delivery-row">
                          <div className="hist-del-main">
                            <StatusBadge status={del.status} />
                            <span className="hist-del-dest">{del.destinationName}</span>
                            <span className={`hist-del-code http-${del.httpStatusCode || 'err'}`}>
                              HTTP {del.httpStatusCode || 'N/A'}
                            </span>
                            <span className="hist-del-attempts font-mono">
                              Attempt {del.attemptCount || 1} / {del.maxAttempts || 3}
                            </span>
                            <span className="hist-del-time">
                              {new Date(del.attemptTimestamp).toLocaleString()}
                            </span>
                            {(del.retryable || del.status === 'FAILED') && (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleRetryDelivery(del.id)}
                                isLoading={retryingDeliveryId === del.id}
                                disabled={retryingDeliveryId !== null || isSending || del.status === 'RETRYING'}
                                title="Retry sending FHIR payload to this destination"
                                style={{ marginLeft: 'auto', padding: '0.2rem 0.6rem', fontSize: '0.75rem' }}
                              >
                                Retry
                              </Button>
                            )}
                          </div>
                          {del.status === 'RETRYING' && del.nextRetryAt && (
                            <div className="hist-del-retrying-notice">
                              <span className="retry-pulse-dot" />
                              <span>
                                <strong>Automatic Retry Scheduled:</strong> Next attempt scheduled for {formatTimestamp(del.nextRetryAt)}
                              </span>
                            </div>
                          )}
                          {del.responseSummary && (
                            <div className="hist-del-summary text-secondary text-sm" style={{ marginTop: '0.25rem', fontStyle: 'italic' }}>
                              ✓ {del.responseSummary}
                            </div>
                          )}
                          {del.errorMessage && (
                            <div className="hist-del-error">{del.errorMessage}</div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <Button variant="secondary" size="md" onClick={() => setSelectedRecord(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
