import React, { useEffect, useRef, useState } from 'react';

export default function InventoryOperationModal({ mode, inventory, label, onClose, onSubmit, busy }) {
  const [quantity, setQuantity] = useState(mode === 'receipt' ? 1 : 0);
  const [reason, setReason] = useState('');
  const headingRef = useRef(null);
  useEffect(() => { headingRef.current?.focus(); }, []);
  const submit = (event) => {
    event.preventDefault();
    if (!window.confirm(`Confirm ${mode === 'receipt' ? 'stock receipt' : 'stock adjustment'}?`)) return;
    onSubmit({ inventoryId: inventory.id, ...(mode === 'receipt'
      ? { quantity: Number(quantity), reason: reason.trim() || null }
      : { quantityChange: Number(quantity), reason: reason.trim() }) });
  };
  return <div className="modal show d-block inventory-modal" role="dialog" aria-modal="true" aria-labelledby="inventory-operation-title">
    <div className="modal-dialog modal-dialog-centered"><div className="modal-content border-0 shadow-lg">
      <div className="modal-header"><div><p className="text-uppercase text-primary small fw-bold mb-1">MF-04 stock operation</p><h2 id="inventory-operation-title" ref={headingRef} tabIndex="-1" className="modal-title h5">{mode === 'receipt' ? 'Record stock receipt' : 'Record stock adjustment'}</h2></div><button type="button" className="btn-close" aria-label="Close" onClick={onClose} /></div>
      <form onSubmit={submit}><div className="modal-body">
        <div className="rounded bg-light p-3 mb-3"><strong>{label}</strong><div className="small text-secondary">Current quantity: {inventory.quantity} doses</div></div>
        <label className="form-label" htmlFor="operation-quantity">{mode === 'receipt' ? 'Quantity received' : 'Quantity change'}</label>
        <input id="operation-quantity" className="form-control" type="number" min={mode === 'receipt' ? 1 : undefined} value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
        {mode === 'adjustment' && <div className="form-text">Use a positive number to add stock or a negative number to remove stock.</div>}
        <label className="form-label mt-3" htmlFor="operation-reason">Reason {mode === 'adjustment' ? '(required)' : '(optional)'}</label>
        <textarea id="operation-reason" className="form-control" rows="3" maxLength="500" value={reason} onChange={(e) => setReason(e.target.value)} required={mode === 'adjustment'} />
      </div><div className="modal-footer"><button type="button" className="btn btn-outline-secondary" onClick={onClose}>Cancel</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Confirm operation'}</button></div></form>
    </div></div>
  </div>;
}
