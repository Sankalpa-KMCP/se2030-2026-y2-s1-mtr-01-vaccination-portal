import React from 'react';
import { Link } from 'react-router-dom';

const FOCUS_TASKS = [
  {
    title: 'Review Screening Queue',
    description: 'Review citizen screening submissions',
    to: '/app/staff/screening/reviews',
    iconClass: 'bi bi-clipboard2-check',
    bg: '#e8f5f1',
    color: '#0f6e5b',
  },
  {
    title: 'Create New Question',
    description: 'Add a clinical question to the catalogue',
    to: '/app/screening/questions',
    iconClass: 'bi bi-plus-lg',
    bg: '#0f6e5b',
    color: '#ffffff',
  },
  {
    title: 'Manage Questionnaires',
    description: 'Author and maintain screening questionnaire versions',
    to: '/app/screening/questionnaires',
    iconClass: 'bi bi-card-checklist',
    bg: '#134e4a',
    color: '#ffffff',
  },
  {
    title: 'View Corrections',
    description: 'Review submissions requiring correction',
    to: '/app/staff/screening/reviews',
    iconClass: 'bi bi-pencil-square',
    bg: '#e8f5f1',
    color: '#0f6e5b',
  },
];

export default function HcoTodayFocus() {
  return (
    <div className="hco-card h-100">
      <div className="hco-card-header">
        <div>
          <h2 className="hco-card-title">
            <i className="bi bi-lightning-charge hco-card-title-icon" aria-hidden="true" />
            <span>Today's Focus</span>
          </h2>
          <div className="small text-secondary mt-1">Quick access to your most common tasks.</div>
        </div>
      </div>

      <div className="hco-card-body">
        {FOCUS_TASKS.map((task) => (
          <Link
            key={task.title}
            to={task.to}
            className="hco-focus-row"
            aria-label={`${task.title}: ${task.description}`}
          >
            <div className="d-flex align-items-center min-w-0">
              <div
                className="hco-focus-icon-container"
                style={{ backgroundColor: task.bg, color: task.color }}
                aria-hidden="true"
              >
                <i className={task.iconClass} />
              </div>
              <div className="min-w-0">
                <div className="hco-focus-title text-truncate">{task.title}</div>
                <div className="hco-focus-desc text-truncate">{task.description}</div>
              </div>
            </div>
            <i className="bi bi-chevron-right hco-focus-chevron" aria-hidden="true" />
          </Link>
        ))}
      </div>
    </div>
  );
}
