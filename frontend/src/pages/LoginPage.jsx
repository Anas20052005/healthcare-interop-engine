import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import Input from '../components/Input';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import apiClient from '../services/apiClient';
import './LoginPage.css';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isWakingUp, setIsWakingUp] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [demoNotice, setDemoNotice] = useState('');
  const [healthStatus, setHealthStatus] = useState(null); // null | 'checking' | 'UP' | 'DOWN'
  const isSubmittingRef = useRef(false);
  const wakeUpTimerRef = useRef(null);

  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    return () => {
      if (wakeUpTimerRef.current) {
        clearTimeout(wakeUpTimerRef.current);
      }
    };
  }, []);

  const handleFillDemoUser = () => {
    setUsername('demo_recruiter');
    setErrorMessage('');
    setDemoNotice(
      'Demo username filled (demo_recruiter). Please enter the demo password configured for this environment to sign in.'
    );
    const pwdInput = document.getElementById('password');
    if (pwdInput) {
      pwdInput.focus();
    }
  };

  const handleCheckHealth = async () => {
    if (healthStatus === 'checking') return;
    setHealthStatus('checking');
    try {
      const res = await apiClient.get('/api/health', { timeout: 8000 });
      if (res.data?.status === 'UP') {
        setHealthStatus('UP');
      } else {
        setHealthStatus('DOWN');
      }
    } catch {
      setHealthStatus('DOWN');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Prevent duplicate submissions and concurrent calls
    if (isLoading || isSubmittingRef.current) {
      return;
    }

    setErrorMessage('');
    setDemoNotice('');

    const cleanUsername = username.trim();
    if (!cleanUsername || !password) {
      setErrorMessage('Please enter both username and password.');
      return;
    }

    isSubmittingRef.current = true;
    setIsLoading(true);
    setIsWakingUp(false);

    // If request takes longer than 4.5 seconds, cloud container is likely waking up
    wakeUpTimerRef.current = setTimeout(() => {
      setIsWakingUp(true);
    }, 4500);

    try {
      await login(cleanUsername, password);
      if (wakeUpTimerRef.current) clearTimeout(wakeUpTimerRef.current);
      isSubmittingRef.current = false;
      setIsLoading(false);
      navigate('/dashboard');
    } catch (err) {
      if (wakeUpTimerRef.current) clearTimeout(wakeUpTimerRef.current);
      isSubmittingRef.current = false;
      setIsLoading(false);
      setIsWakingUp(false);

      const msg = err.userMessage || err.message || 'Invalid username or password.';
      setErrorMessage(msg);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-card">
          <div className="login-brand-header">
            <div className="login-logo-container">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z" />
              </svg>
            </div>
            <h2 className="login-title">HL7 → FHIR Engine</h2>
            <p className="login-subtitle">Healthcare Interoperability Gateway</p>
          </div>

          {errorMessage && (
            <ErrorMessage
              title="Authentication Error"
              message={errorMessage}
              onDismiss={() => setErrorMessage('')}
              className="login-alert"
            />
          )}

          {isWakingUp && (
            <div className="cold-start-banner" role="status">
              <div className="cold-start-spinner" />
              <div>
                <strong>Backend waking up:</strong> The Render hosting container is spinning up from idle state.
                This initial wake-up may take 25–40 seconds. Please hold on...
              </div>
            </div>
          )}

          {demoNotice && (
            <div className="demo-notice-banner" role="status">
              <span className="demo-notice-icon">ℹ</span>
              <span>{demoNotice}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="login-form">
            <Input
              id="username"
              name="username"
              label="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter username"
              required
              autoComplete="username"
              disabled={isLoading}
            />

            <Input
              id="password"
              name="password"
              label="Password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
              required
              autoComplete="current-password"
              disabled={isLoading}
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

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={isLoading}
              disabled={isLoading}
              className="login-submit-btn"
            >
              {isLoading ? (isWakingUp ? 'Waking Server...' : 'Signing In...') : 'Sign In to Interop Engine'}
            </Button>
          </form>

          <div className="login-footer-tools">
            <button
              type="button"
              className="health-probe-btn"
              onClick={handleCheckHealth}
              disabled={healthStatus === 'checking'}
              aria-label="Check backend service connectivity"
            >
              {healthStatus === 'checking' && 'Checking backend status...'}
              {healthStatus === 'UP' && '● Backend is Online'}
              {healthStatus === 'DOWN' && '○ Backend is Idle / Starting'}
              {healthStatus === null && 'Probe Backend Status'}
            </button>
          </div>
        </div>

        {/* Recruiter & Evaluator Demo Sandbox Card */}
        <div className="recruiter-demo-card">
          <div className="recruiter-badge-container">
            <span className="recruiter-pill">Recruiter Demo</span>
            <span className="sandbox-pill">Isolated Sandbox</span>
          </div>

          <h3 className="recruiter-title">Recruiter & Evaluator Access</h3>
          <p className="recruiter-desc">
            Evaluating this project? A dedicated demo account is provisioned to explore real-time HL7 v2
            to FHIR R4 mapping, synthetic patient conversion, audit logs, and dashboard metrics without
            production administrator credentials.
          </p>

          <div className="recruiter-account-box">
            <div className="recruiter-account-row">
              <span className="account-label">Username:</span>
              <code className="account-value">demo_recruiter</code>
            </div>
            <div className="recruiter-account-row">
              <span className="account-label">Password:</span>
              <span className="account-help-text">Configured via secure backend secret (<code>DEMO_PASSWORD</code>)</span>
            </div>
          </div>

          <div className="recruiter-action-row">
            <button
              type="button"
              className="use-demo-btn"
              onClick={handleFillDemoUser}
              disabled={isLoading}
            >
              Use Demo Account
            </button>
            <span className="recruiter-hint">
              Sets username and focuses password field. Does not auto-submit.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
