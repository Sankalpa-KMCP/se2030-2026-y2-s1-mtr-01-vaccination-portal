import React, { useRef } from 'react';

/**
 * Accessible journey switcher. Implements the WAI-ARIA Tabs pattern:
 * roving tabindex, arrow-key navigation, Home/End support.
 */
export default function JourneyTabs({ tabs, activeId, onChange }) {
  const tabRefs = useRef({});

  const focusTab = (id) => {
    tabRefs.current[id]?.focus();
  };

  const handleKeyDown = (event, index) => {
    const lastIndex = tabs.length - 1;
    let nextIndex = null;

    switch (event.key) {
      case 'ArrowRight':
        nextIndex = index === lastIndex ? 0 : index + 1;
        break;
      case 'ArrowLeft':
        nextIndex = index === 0 ? lastIndex : index - 1;
        break;
      case 'Home':
        nextIndex = 0;
        break;
      case 'End':
        nextIndex = lastIndex;
        break;
      default:
        return;
    }

    event.preventDefault();
    const nextTab = tabs[nextIndex];
    onChange(nextTab.id);
    focusTab(nextTab.id);
  };

  return (
    <div className="lp-journey-tabs" role="tablist" aria-label="Choose a journey">
      {tabs.map((tab, index) => {
        const isActive = tab.id === activeId;
        return (
          <button
            key={tab.id}
            ref={(el) => { tabRefs.current[tab.id] = el; }}
            type="button"
            role="tab"
            id={`lp-journey-tab-${tab.id}`}
            aria-selected={isActive}
            aria-controls={`lp-journey-panel-${tab.id}`}
            tabIndex={isActive ? 0 : -1}
            className={`lp-journey-tabs__tab${isActive ? ' lp-is-active' : ''}`}
            onClick={() => onChange(tab.id)}
            onKeyDown={(event) => handleKeyDown(event, index)}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
