import React, { useId, useState } from 'react';

function FaqItem({ question, answer, index, open, onToggle }) {
  const buttonId = useId();
  const regionId = useId();

  return (
    <div className="lp-faq__item">
      <h3 className="lp-faq__heading">
        <button
          type="button"
          id={buttonId}
          className={`lp-faq__trigger${open ? ' lp-is-open' : ''}`}
          aria-expanded={open}
          aria-controls={regionId}
          onClick={onToggle}
        >
          <span className="lp-faq__q-index" aria-hidden="true">
            {String(index + 1).padStart(2, '0')}
          </span>
          <span className="lp-faq__q-text">{question}</span>
          <span className="lp-faq__chevron" aria-hidden="true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
        </button>
      </h3>
      <div
        id={regionId}
        role="region"
        aria-labelledby={buttonId}
        className={`lp-faq__panel${open ? ' lp-is-open' : ''}`}
      >
        <div className="lp-faq__answer">
          <p>{answer}</p>
        </div>
      </div>
    </div>
  );
}

/**
 * Accessible FAQ accordion — one item open at a time, proper
 * button/region wiring, keyboard-native (real <button> elements).
 */
export default function FaqAccordion({ items }) {
  const [openIndex, setOpenIndex] = useState(0);

  return (
    <div className="lp-faq">
      {items.map((item, index) => (
        <FaqItem
          key={item.question}
          index={index}
          question={item.question}
          answer={item.answer}
          open={openIndex === index}
          onToggle={() => setOpenIndex(openIndex === index ? -1 : index)}
        />
      ))}
    </div>
  );
}
