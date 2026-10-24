import React from 'react';
import './Card.css';

/**
 * Reusable Card Container
 */
export default function Card({
  title,
  subtitle,
  actions,
  children,
  footer,
  className = '',
  bodyClassName = '',
}) {
  return (
    <div className={`card-container ${className}`}>
      {(title || subtitle || actions) && (
        <div className="card-header">
          <div className="card-header-titles">
            {title && <h3 className="card-title">{title}</h3>}
            {subtitle && <p className="card-subtitle">{subtitle}</p>}
          </div>
          {actions && <div className="card-header-actions">{actions}</div>}
        </div>
      )}
      <div className={`card-body ${bodyClassName}`}>{children}</div>
      {footer && <div className="card-footer">{footer}</div>}
    </div>
  );
}
