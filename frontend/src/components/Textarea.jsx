import React from 'react';
import './Textarea.css';

/**
 * Reusable Accessible Textarea Component
 * Supports monospace font for HL7 messages
 */
export default function Textarea({
  id,
  name,
  label,
  value,
  onChange,
  placeholder,
  error,
  helperText,
  rows = 10,
  monospace = false,
  required = false,
  disabled = false,
  className = '',
  ...rest
}) {
  return (
    <div className={`textarea-group ${error ? 'has-error' : ''} ${className}`}>
      {label && (
        <label htmlFor={id} className="textarea-label">
          {label}
          {required && <span className="required-star" aria-hidden="true">*</span>}
        </label>
      )}
      <textarea
        id={id}
        name={name || id}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        rows={rows}
        disabled={disabled}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : helperText ? `${id}-helper` : undefined}
        className={`textarea-field ${monospace ? 'textarea-mono' : ''}`}
        {...rest}
      />
      {error && (
        <p id={`${id}-error`} className="textarea-error-msg" role="alert">
          {error}
        </p>
      )}
      {!error && helperText && (
        <p id={`${id}-helper`} className="textarea-helper-msg">
          {helperText}
        </p>
      )}
    </div>
  );
}
