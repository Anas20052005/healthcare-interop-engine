import React, { useState, useEffect } from 'react';
import Textarea from '../components/Textarea';
import Button from '../components/Button';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';
import FHIRViewer from '../components/FHIRViewer';
import { SAMPLE_HL7_MESSAGE } from '../services/mockData';
import conversionService from '../services/conversionService';
import destinationService from '../services/destinationService';
import './ConvertPage.css';

export default function ConvertPage() {
  const [hl7Input, setHl7Input] = useState(SAMPLE_HL7_MESSAGE);
  const [isConverting, setIsConverting] = useState(false);
  const [conversionResult, setConversionResult] = useState(null);
  const [conversionError, setConversionError] = useState('');

  // Destination Sending State
  const [destinations, setDestinations] = useState([]);
  const [selectedDestinationId, setSelectedDestinationId] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [deliveryResult, setDeliveryResult] = useState(null);
  const [deliveryError, setDeliveryError] = useState('');
  const [deliveriesList, setDeliveriesList] = useState([]);
  const [showDeliveryHistory, setShowDeliveryHistory] = useState(false);


  useEffect(() => {
    let ignore = false;
    async function fetchDestinations() {
      try {
        const list = await destinationService.getDestinations();
        if (ignore) return;
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
        if (!ignore) {
          console.error('Failed to load destinations in ConvertPage', e);
        }
      }
    }
    fetchDestinations();
    return () => {
      ignore = true;
    };
  }, []);

  // Handle Load Example
  const handleLoadExample = () => {
    setHl7Input(SAMPLE_HL7_MESSAGE);
    setConversionError('');
  };

  // Handle Clear
  const handleClear = () => {
    setHl7Input('');
    setConversionResult(null);
    setConversionError('');
    setDeliveryResult(null);
    setDeliveryError('');
    setDeliveriesList([]);
  };

  // Handle Convert
  const handleConvert = async (e) => {
    if (e) e.preventDefault();
    setConversionError('');
    setDeliveryResult(null);
    setDeliveryError('');
    setDeliveriesList([]);

    if (!hl7Input || !hl7Input.trim()) {
      setConversionError('HL7 message is required. Please paste or enter an HL7 message to convert.');
      return;
    }

    setIsConverting(true);

    try {
      const result = await conversionService.convertHL7(hl7Input);
      setConversionResult(result);
      setIsConverting(false);
    } catch (err) {
      setIsConverting(false);
      setConversionError(err.userMessage || err.message || 'HL7 conversion failed.');
      setConversionResult(null);
    }
  };

  // Handle Send to Destination
  const handleSendToDestination = async () => {
    if (!conversionResult || !conversionResult.conversionId) return;
    if (!selectedDestinationId) {
      setDeliveryError('Please select a destination endpoint first.');
      return;
    }

    const targetDest = destinations.find((d) => String(d.id) === String(selectedDestinationId));
    const targetName = targetDest ? targetDest.name : `Destination #${selectedDestinationId}`;
    const confirmed = window.confirm(
      `Confirm Delivery: Send generated FHIR R4 output (Conversion #${conversionResult.conversionId}) to "${targetName}"?`
    );
    if (!confirmed) return;

    setIsSending(true);
    setDeliveryError('');
    try {
      const result = await destinationService.sendConversion(conversionResult.conversionId, selectedDestinationId);
      setDeliveryResult(result);
      const list = await destinationService.getConversionDeliveries(conversionResult.conversionId);
      setDeliveriesList(Array.isArray(list) ? list : []);
    } catch (err) {
      setDeliveryError(err.userMessage || err.message || 'Failed to dispatch conversion to destination.');
      try {
        const list = await destinationService.getConversionDeliveries(conversionResult.conversionId);
        setDeliveriesList(Array.isArray(list) ? list : []);
      } catch {
        // Ignore failure to load deliveries list
      }
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="convert-page">
      {/* Page Header Intro */}
      <div className="convert-intro">
        <h2 className="convert-intro-title">HL7 v2 to FHIR R4 Translation Console</h2>
        <p className="convert-intro-desc">
          Enter a raw HL7 v2 clinical message (ADT^A01) to validate, parse segments (MSH, PID, PV1, OBX), and generate compliant FHIR R4 resource bundles.
        </p>
      </div>

      {conversionError && (
        <ErrorMessage
          title="Conversion Failed"
          message={conversionError}
          onDismiss={() => setConversionError('')}
          className="convert-alert"
        />
      )}

      {/* Main Grid: Input on Left, Output on Right */}
      <div className="convert-panels-grid">
        {/* Left Panel: HL7 Input */}
        <div className="convert-panel panel-input">
          <Card
            title="HL7 v2 Message Input"
            subtitle="Paste pipe-delimited HL7 message string"
            actions={
              <div className="input-header-actions">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleLoadExample}
                  disabled={isConverting}
                  title="Populate valid ADT^A01 sample"
                >
                  Load Example
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleClear}
                  disabled={isConverting || !hl7Input}
                  title="Clear input and results"
                >
                  Clear
                </Button>
              </div>
            }
          >
            <form onSubmit={handleConvert} className="convert-form">
              <Textarea
                id="hl7Input"
                name="hl7Input"
                value={hl7Input}
                onChange={(e) => setHl7Input(e.target.value)}
                placeholder="MSH|^~\&|HIS|HOSPITAL|...&#10;PID|1||P1001||Surname^Name...&#10;PV1|1|I|WARD1...&#10;OBX|1|NM|GLUCOSE||120|mg/dL..."
                rows={14}
                monospace
                disabled={isConverting}
                className="hl7-textarea-field"
              />

              <div className="convert-form-actions">
                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  isLoading={isConverting}
                  className="btn-convert-cta"
                >
                  Convert to FHIR R4
                </Button>
                <span className="hl7-segment-counter">
                  {hl7Input.trim() ? `${hl7Input.trim().split(/\r\n|\r|\n/).filter(Boolean).length} segments detected` : 'No segments'}
                </span>
              </div>
            </form>
          </Card>
        </div>

        {/* Right Panel: Conversion Results & FHIR Output */}
        <div className="convert-panel panel-output">
          {conversionResult ? (
            <div className="result-container">
              {/* Summary Metadata Strip */}
              <div className="conversion-meta-strip">
                <div className="meta-strip-header">
                  <div className="meta-status-wrap">
                    <StatusBadge status={conversionResult.status} />
                    <span className="meta-conv-id">ID #{conversionResult.conversionId}</span>
                  </div>
                  <span className="meta-timestamp">{new Date(conversionResult.createdAt).toLocaleTimeString()}</span>
                </div>

                <div className="meta-chips-grid">
                  <div className="meta-chip">
                    <span className="chip-label">Control ID</span>
                    <span className="chip-value font-mono">{conversionResult.messageControlId}</span>
                  </div>
                  <div className="meta-chip">
                    <span className="chip-label">Message Type</span>
                    <span className="chip-value font-mono">{conversionResult.messageType}</span>
                  </div>
                  <div className="meta-chip">
                    <span className="chip-label">Patient ID</span>
                    <span className="chip-value font-mono">{conversionResult.patientId}</span>
                  </div>
                  <div className="meta-chip">
                    <span className="chip-label">Encounter ID</span>
                    <span className="chip-value font-mono">{conversionResult.encounterId || 'N/A'}</span>
                  </div>
                  <div className="meta-chip">
                    <span className="chip-label">Observations</span>
                    <span className="chip-value font-mono">{conversionResult.observationCount}</span>
                  </div>
                </div>
              </div>

              {/* FHIR Output Viewer */}
              <FHIRViewer
                fhirData={conversionResult.fhirBundle}
                title={`FHIR Bundle (${conversionResult.patientId})`}
              />

              {/* Priority 5: Dispatch to Destination Server Card */}
              <Card
                title="Dispatch to Destination Server"
                subtitle="Send the validated FHIR R4 Bundle directly to an external or mock FHIR destination"
                className="dispatch-card"
              >
                {deliveryError && (
                  <ErrorMessage
                    title="Delivery Error"
                    message={deliveryError}
                    onDismiss={() => setDeliveryError('')}
                  />
                )}

                {destinations.length === 0 ? (
                  <div className="no-destinations-hint">
                    <p>No active destinations are currently configured.</p>
                    <a href="/destinations" className="configure-dest-link">
                      Configure a Destination &rarr;
                    </a>
                  </div>
                ) : (
                  <div className="dispatch-form-layout">
                    <div className="dispatch-input-row">
                      <div className="dispatch-select-wrap">
                        <label className="dispatch-label">Target Destination</label>
                        <select
                          className="dispatch-select"
                          value={selectedDestinationId}
                          onChange={(e) => setSelectedDestinationId(e.target.value)}
                          disabled={isSending}
                        >
                          {destinations.map((d) => (
                            <option key={d.id} value={d.id}>
                              {d.name} ({d.baseUrl}) {d.mock ? '[Mock Server]' : ''}
                            </option>
                          ))}
                        </select>
                      </div>

                      <Button
                        variant="primary"
                        onClick={handleSendToDestination}
                        isLoading={isSending}
                        disabled={!selectedDestinationId}
                        className="btn-send-dispatch"
                      >
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="btn-icon">
                          <line x1="22" y1="2" x2="11" y2="13" />
                          <polygon points="22 2 15 22 11 13 2 9 22 2" />
                        </svg>
                        Send to Destination
                      </Button>
                    </div>

                    {deliveryResult && (
                      <div className={`delivery-result-banner delivery-${deliveryResult.status.toLowerCase()}`}>
                        <div className="delivery-banner-top">
                          <div className="delivery-banner-left">
                            <StatusBadge status={deliveryResult.status} />
                            <span className="delivery-dest-name">{deliveryResult.destinationName}</span>
                            <span className={`http-code-pill http-${deliveryResult.httpStatusCode || 'err'}`}>
                              HTTP {deliveryResult.httpStatusCode || 'N/A'}
                            </span>
                          </div>
                          <span className="delivery-timestamp">
                            {new Date(deliveryResult.attemptTimestamp).toLocaleTimeString()}
                          </span>
                        </div>
                        {deliveryResult.errorMessage && (
                          <div className="delivery-error-detail">
                            {deliveryResult.errorMessage}
                          </div>
                        )}
                        {deliveryResult.status === 'SUCCESS' && (
                          <div className="delivery-success-note">
                            Resource successfully received and acknowledged by destination server.
                            {deliveryResult.responseSummary && (
                              <div style={{ marginTop: '0.25rem', opacity: 0.9 }}>
                                Summary: {deliveryResult.responseSummary}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    )}

                    {deliveriesList.length > 1 && (
                      <div className="deliveries-history-toggle">
                        <button
                          type="button"
                          className="btn-toggle-deliveries"
                          onClick={() => setShowDeliveryHistory(!showDeliveryHistory)}
                        >
                          {showDeliveryHistory ? 'Hide' : 'Show'} past delivery attempts for this conversion ({deliveriesList.length})
                        </button>
                        {showDeliveryHistory && (
                          <div className="deliveries-sublist">
                            {deliveriesList.map((d) => (
                              <div key={d.id} className="delivery-subitem">
                                <StatusBadge status={d.status} />
                                <span className="subitem-dest">{d.destinationName}</span>
                                <span className="subitem-code">HTTP {d.httpStatusCode || 'N/A'}</span>
                                <span className="subitem-time">{new Date(d.attemptTimestamp).toLocaleTimeString()}</span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </Card>
            </div>
          ) : (
            <Card className="empty-output-card">
              <EmptyState
                title="Awaiting HL7 Input"
                description="Paste an HL7 v2 message on the left and click 'Convert to FHIR R4' to preview the generated clinical resources."
                action={
                  <Button variant="secondary" size="sm" onClick={handleLoadExample}>
                    Load Synthetic ADT^A01 Sample
                  </Button>
                }
              />
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
