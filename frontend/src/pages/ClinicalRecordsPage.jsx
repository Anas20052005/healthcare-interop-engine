import React, { useState, useEffect, useCallback, useMemo } from 'react';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import Button from '../components/Button';
import Input from '../components/Input';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import patientService from '../services/patientService';
import './ClinicalRecordsPage.css';

export default function ClinicalRecordsPage() {
  const [patients, setPatients] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Selected patient details
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [encounters, setEncounters] = useState([]);
  const [observations, setObservations] = useState([]);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [detailsError, setDetailsError] = useState(null);
  const [activeTab, setActiveTab] = useState('demographics'); // 'demographics', 'encounters', 'observations'

  const loadPatients = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await patientService.getPatients();
      setPatients(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.userMessage || err.message || 'Failed to load clinical patient records');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    async function fetchInitial() {
      try {
        const data = await patientService.getPatients();
        if (!ignore) {
          setPatients(Array.isArray(data) ? data : []);
          setIsLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          setError(err.userMessage || err.message || 'Failed to load clinical patient records');
          setIsLoading(false);
        }
      }
    }
    fetchInitial();
    return () => {
      ignore = true;
    };
  }, []);

  const handleSelectPatient = async (patient) => {
    setSelectedPatient(patient);
    setActiveTab('demographics');
    setIsLoadingDetails(true);
    setDetailsError(null);
    try {
      const [encData, obsData] = await Promise.all([
        patientService.getPatientEncounters(patient.id),
        patientService.getPatientObservations(patient.id),
      ]);
      setEncounters(Array.isArray(encData) ? encData : []);
      setObservations(Array.isArray(obsData) ? obsData : []);
    } catch (err) {
      setDetailsError(err.userMessage || err.message || 'Failed to load patient encounters and observations');
    } finally {
      setIsLoadingDetails(false);
    }
  };

  const filteredPatients = useMemo(() => {
    if (!searchQuery.trim()) return patients;
    const q = searchQuery.toLowerCase();
    return patients.filter((p) => {
      const idMatch = p.patientIdentifier?.toLowerCase().includes(q);
      const nameMatch = `${p.givenName || ''} ${p.familyName || ''}`.toLowerCase().includes(q);
      const phoneMatch = p.phoneNumber?.toLowerCase().includes(q);
      return idMatch || nameMatch || phoneMatch;
    });
  }, [patients, searchQuery]);

  return (
    <div className="clinical-records-page">
      <div className="clinical-header">
        <div>
          <h1 className="clinical-title">Clinical Patient Records</h1>
          <p className="clinical-subtitle">
            Browse ingested patients, encounters, and lab observations strictly scoped to your hospital tenant.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={loadPatients} isLoading={isLoading}>
          Refresh List
        </Button>
      </div>

      {error && <ErrorMessage title="Failed to Load Patients" message={error} onDismiss={() => setError(null)} />}

      <div className="clinical-layout">
        {/* Patient Master List */}
        <div className="patient-list-column">
          <Card title="Patient Registry" subtitle={`Showing ${filteredPatients.length} record(s)`}>
            <div className="search-box">
              <Input
                placeholder="Search MRN, name, or phone..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            {isLoading ? (
              <div className="clinical-loading">
                <LoadingSpinner />
                <span>Loading patient registry...</span>
              </div>
            ) : filteredPatients.length === 0 ? (
              <EmptyState
                title="No Clinical Records Found"
                message={searchQuery ? 'No patients matched your search filter.' : 'No patients ingested yet. Convert an HL7 message to populate records.'}
              />
            ) : (
              <div className="patient-items-container">
                {filteredPatients.map((p) => {
                  const isSelected = selectedPatient?.id === p.id;
                  return (
                    <div
                      key={p.id}
                      className={`patient-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleSelectPatient(p)}
                    >
                      <div className="patient-item-main">
                        <span className="patient-name">
                          {p.familyName ? `${p.familyName}, ${p.givenName || ''}` : p.patientIdentifier}
                        </span>
                        <span className="patient-mrn">MRN: {p.patientIdentifier}</span>
                      </div>
                      <div className="patient-item-meta">
                        <span className="patient-gender">{p.gender || 'Unknown'}</span>
                        {p.birthDate && <span className="patient-dob">DOB: {p.birthDate}</span>}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>

        {/* Patient Clinical Detail View */}
        <div className="patient-detail-column">
          {selectedPatient ? (
            <Card
              title={`${selectedPatient.familyName || 'Patient'}, ${selectedPatient.givenName || ''}`}
              subtitle={`MRN: ${selectedPatient.patientIdentifier} | Tenant: ${selectedPatient.hospitalName || 'Hospital Scope'}`}
            >
              {/* Tab Navigation */}
              <div className="clinical-tab-bar">
                <button
                  type="button"
                  className={`tab-btn ${activeTab === 'demographics' ? 'active' : ''}`}
                  onClick={() => setActiveTab('demographics')}
                >
                  Demographics
                </button>
                <button
                  type="button"
                  className={`tab-btn ${activeTab === 'encounters' ? 'active' : ''}`}
                  onClick={() => setActiveTab('encounters')}
                >
                  Encounters ({encounters.length})
                </button>
                <button
                  type="button"
                  className={`tab-btn ${activeTab === 'observations' ? 'active' : ''}`}
                  onClick={() => setActiveTab('observations')}
                >
                  Observations ({observations.length})
                </button>
              </div>

              {isLoadingDetails ? (
                <div className="clinical-loading">
                  <LoadingSpinner />
                  <span>Loading clinical history...</span>
                </div>
              ) : detailsError ? (
                <ErrorMessage message={detailsError} />
              ) : (
                <div className="tab-content-area">
                  {/* Demographics Tab */}
                  {activeTab === 'demographics' && (
                    <div className="demographics-grid">
                      <div className="detail-item">
                        <label>Patient Identifier (MRN)</label>
                        <span>{selectedPatient.patientIdentifier}</span>
                      </div>
                      <div className="detail-item">
                        <label>Full Legal Name</label>
                        <span>{selectedPatient.givenName} {selectedPatient.familyName}</span>
                      </div>
                      <div className="detail-item">
                        <label>Date of Birth</label>
                        <span>{selectedPatient.birthDate || 'Not recorded'}</span>
                      </div>
                      <div className="detail-item">
                        <label>Administrative Gender</label>
                        <span>{selectedPatient.gender || 'Not specified'}</span>
                      </div>
                      <div className="detail-item">
                        <label>Phone Number</label>
                        <span>{selectedPatient.phoneNumber || 'Not provided'}</span>
                      </div>
                      <div className="detail-item">
                        <label>Residential Address</label>
                        <span>
                          {[selectedPatient.addressLine, selectedPatient.city, selectedPatient.state, selectedPatient.postalCode]
                            .filter(Boolean)
                            .join(', ') || 'Not recorded'}
                        </span>
                      </div>
                      <div className="detail-item">
                        <label>Ingested On</label>
                        <span>{selectedPatient.createdAt ? new Date(selectedPatient.createdAt).toLocaleString() : 'N/A'}</span>
                      </div>
                    </div>
                  )}

                  {/* Encounters Tab */}
                  {activeTab === 'encounters' && (
                    <div className="encounters-section">
                      {encounters.length === 0 ? (
                        <EmptyState title="No Encounters" message="No encounters recorded for this patient." />
                      ) : (
                        <div className="table-responsive">
                          <table className="clinical-table">
                            <thead>
                              <tr>
                                <th>Encounter ID</th>
                                <th>Class</th>
                                <th>Location</th>
                                <th>Admit Time</th>
                                <th>Discharge Time</th>
                                <th>Attending Doctor</th>
                              </tr>
                            </thead>
                            <tbody>
                              {encounters.map((enc) => (
                                <tr key={enc.id}>
                                  <td className="mono">{enc.encounterIdentifier || `ENC-${enc.id}`}</td>
                                  <td>
                                    <StatusBadge status={enc.patientClass === 'I' ? 'INPATIENT' : enc.patientClass === 'E' ? 'EMERGENCY' : 'OUTPATIENT'} />
                                  </td>
                                  <td>{enc.location || 'N/A'}</td>
                                  <td>{enc.admitDateTime ? new Date(enc.admitDateTime).toLocaleString() : 'N/A'}</td>
                                  <td>{enc.dischargeDateTime ? new Date(enc.dischargeDateTime).toLocaleString() : 'Active'}</td>
                                  <td>{enc.attendingDoctor || 'N/A'}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Observations Tab */}
                  {activeTab === 'observations' && (
                    <div className="observations-section">
                      {observations.length === 0 ? (
                        <EmptyState title="No Observations" message="No lab observations or vital signs recorded for this patient." />
                      ) : (
                        <div className="table-responsive">
                          <table className="clinical-table">
                            <thead>
                              <tr>
                                <th>Code / Test</th>
                                <th>Type</th>
                                <th>Observed Value</th>
                                <th>Units</th>
                                <th>Reference Range</th>
                                <th>Flags</th>
                                <th>Status</th>
                                <th>Recorded At</th>
                              </tr>
                            </thead>
                            <tbody>
                              {observations.map((obs) => (
                                <tr key={obs.id}>
                                  <td className="mono font-semibold">{obs.observationCode}</td>
                                  <td>{obs.valueType}</td>
                                  <td className="mono">
                                    {obs.valueNumeric !== null && obs.valueNumeric !== undefined
                                      ? obs.valueNumeric
                                      : obs.valueString || 'N/A'}
                                  </td>
                                  <td>{obs.units || '—'}</td>
                                  <td>{obs.referenceRange || '—'}</td>
                                  <td>
                                    {obs.abnormalFlags ? (
                                      <span className="badge-flag abnormal">{obs.abnormalFlags}</span>
                                    ) : (
                                      <span className="badge-flag normal">NORMAL</span>
                                    )}
                                  </td>
                                  <td>
                                    <StatusBadge status={obs.status || 'FINAL'} />
                                  </td>
                                  <td>{obs.observationDateTime ? new Date(obs.observationDateTime).toLocaleString() : 'N/A'}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </Card>
          ) : (
            <Card className="empty-selection-card">
              <div className="select-prompt">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                <h3>Select a Patient</h3>
                <p>Choose a patient from the registry on the left to inspect demographics, visits, and clinical lab observations.</p>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
