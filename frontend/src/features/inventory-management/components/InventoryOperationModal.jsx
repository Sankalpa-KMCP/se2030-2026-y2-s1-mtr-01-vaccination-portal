import { useEffect, useRef, useState } from 'react';

const titles = { receipt: 'Record stock receipt', adjustment: 'Record stock adjustment', transfer: 'Transfer stock' };

export default function InventoryOperationModal({ mode, inventory, label, centers = [], onClose, onSubmit, busy }) {
  const [quantity, setQuantity] = useState(mode === 'adjustment' ? '' : '1');
  const [destinationCenterId, setDestinationCenterId] = useState('');
  const [reason, setReason] = useState('');
  const [validation, setValidation] = useState('');
  const headingRef = useRef(null);

  useEffect(() => {
    headingRef.current?.focus();
    const onKeyDown = (event) => { if (event.key === 'Escape' && !busy) onClose(); };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [busy, onClose]);

  const submit = (event) => {
    event.preventDefault();
    const amount = Number(quantity);
    if (!Number.isInteger(amount) || amount === 0 || (mode !== 'adjustment' && amount < 1)) {
      setValidation(mode === 'adjustment' ? 'Enter a non-zero whole-number change.' : 'Enter a positive whole number of doses.');
      return;
    }
    if (mode === 'adjustment' && inventory.quantity + amount < 0) {
      setValidation(`Only ${inventory.quantity} doses are available to remove.`);
      return;
    }
    if (mode === 'transfer' && (!destinationCenterId || amount > inventory.quantity)) {
      setValidation(amount > inventory.quantity ? `Only ${inventory.quantity} doses are available to transfer.` : 'Choose a destination center.');
      return;
    }
    if (mode === 'adjustment' && !reason.trim()) {
      setValidation('A reason is required for stock adjustments.');
      return;
    }
    setValidation('');
    if (!window.confirm(`Confirm ${titles[mode].toLowerCase()} of ${Math.abs(amount)} dose${Math.abs(amount) === 1 ? '' : 's'}?`)) return;
    const body = mode === 'receipt'
      ? { inventoryId: inventory.id, quantity: amount, reason: reason.trim() || null }
      : mode === 'adjustment'
        ? { inventoryId: inventory.id, quantityChange: amount, reason: reason.trim() }
        : { inventoryId: inventory.id, destinationCenterId: Number(destinationCenterId), quantity: amount, reason: reason.trim() || null };
    onSubmit(body);
  };

  return <div className="modal show d-block inventory-modal" role="dialog" aria-modal="true" aria-labelledby="inventory-operation-title">
    <div className="modal-dialog modal-dialog-centered"><div className="modal-content border-0 shadow-lg">
      <div className="modal-header"><div><p className="text-uppercase text-primary small fw-bold mb-1">MF-04 stock operation</p><h2 id="inventory-operation-title" ref={headingRef} tabIndex="-1" className="modal-title h5">{titles[mode]}</h2></div><button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={busy}/></div>
      <form onSubmit={submit}><div className="modal-body">
        <div className="inventory-operation-context mb-3"><strong>{label}</strong><span>Current quantity: {inventory.quantity} doses</span></div>
        {mode === 'transfer' && <div className="mb-3"><label className="form-label fw-semibold" htmlFor="transfer-destination">Destination center</label><select id="transfer-destination" className="form-select" value={destinationCenterId} onChange={(event) => setDestinationCenterId(event.target.value)} required><option value="">Select a different center</option>{centers.filter((center) => center.id !== inventory.centerId).map((center) => <option key={center.id} value={center.id}>{center.name}</option>)}</select></div>}
        <label className="form-label fw-semibold" htmlFor="operation-quantity">{mode === 'receipt' ? 'Quantity received' : mode === 'transfer' ? 'Quantity to transfer' : 'Quantity change'}</label>
        <input id="operation-quantity" className="form-control" type="number" step="1" min={mode === 'adjustment' ? undefined : 1} max={mode === 'transfer' ? inventory.quantity : undefined} value={quantity} onChange={(event) => setQuantity(event.target.value)} required />
        {mode === 'adjustment' && <div className="form-text">Use a positive number to add doses or a negative number to remove doses.</div>}
        <label className="form-label fw-semibold mt-3" htmlFor="operation-reason">Reason {mode === 'adjustment' ? '(required)' : '(optional)'}</label><textarea id="operation-reason" className="form-control" rows="3" maxLength="500" value={reason} onChange={(event) => setReason(event.target.value)} required={mode === 'adjustment'} />
        {validation && <p className="text-danger small mt-2 mb-0" role="alert">{validation}</p>}
        {mode === 'transfer' && <p className="form-text mb-0 mt-3">Both center balances and their audit records are saved together.</p>}
      </div><div className="modal-footer"><button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={busy}>Cancel</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Confirm operation'}</button></div></form>
    </div></div>
  </div>;
}
