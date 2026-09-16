import React, { useCallback, useEffect, useMemo, useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import EmptyState from '../../../components/common/EmptyState';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import SuccessAlert from '../../../components/common/SuccessAlert';
import { inventoryApi } from '../api/inventoryApi';
import InventoryOperationModal from '../components/InventoryOperationModal';
import '../inventory-management.css';

const LOW_STOCK = 10;
const NEAR_EXPIRY = 30;
const STATUSES = ['ACTIVE', 'QUARANTINED', 'RECALLED', 'EXPIRED', 'INACTIVE'];
const EMPTY = { manufacturers: [], batches: [], stocks: [], transactions: [], references: { vaccineTypes: [], centers: [] } };
const blankManufacturer = { code: '', name: '' };
const blankBatch = { manufacturerId: '', vaccineTypeId: '', batchNumber: '', manufactureDate: '', expiryDate: '', batchStatus: 'ACTIVE' };
const blankStock = { centerId: '', batchId: '', quantity: 0 };
const daysUntil = (date) => Math.ceil((new Date(`${date}T00:00:00`) - new Date()) / 86400000);
const title = (value) => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
const badge = (status) => ({ ACTIVE: 'text-bg-success', QUARANTINED: 'text-bg-warning', RECALLED: 'text-bg-danger', EXPIRED: 'text-bg-dark', INACTIVE: 'text-bg-secondary' }[status] || 'text-bg-secondary');

function SummaryCard({ icon, label, value, note, tone = 'primary' }) {
  return <div className="col-12 col-sm-6 col-xl"><article className="card summary-card shadow-sm h-100"><div className="card-body d-flex justify-content-between gap-3"><div><p className="small text-uppercase fw-semibold text-secondary mb-2">{label}</p><div className="display-6 fw-bold lh-1">{value}</div><p className="small text-secondary mb-0 mt-2">{note}</p></div><span className={`summary-icon bg-${tone}-subtle text-${tone}`}><i className={`bi ${icon}`} aria-hidden="true" /></span></div></article></div>;
}

function SelectField({ id, label, value, onChange, options, all = false, disabled = false }) {
  return <div><label className="form-label small fw-semibold" htmlFor={id}>{label}</label><select id={id} className="form-select" value={value} onChange={onChange} disabled={disabled} required={!all}><option value="">{all ? `All ${label.toLowerCase()}` : `Select ${label.toLowerCase()}`}</option>{options.map((x) => <option key={x.id ?? x} value={x.id ?? x}>{x.code ? `${x.code} — ${x.name}` : title(x)}</option>)}</select></div>;
}

export default function InventoryManagementPage() {
  const [data, setData] = useState(EMPTY);
  const [view, setView] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState('');
  const [refreshedAt, setRefreshedAt] = useState(null);
  const [filters, setFilters] = useState({ search: '', center: '', type: '', manufacturer: '', status: '', low: false, expiry: false });
  const [sort, setSort] = useState({ field: 'expiryDate', direction: 'asc' });
  const [manufacturerForm, setManufacturerForm] = useState(blankManufacturer);
  const [batchForm, setBatchForm] = useState(blankBatch);
  const [stockForm, setStockForm] = useState(blankStock);
  const [editingManufacturer, setEditingManufacturer] = useState(null);
  const [editingBatch, setEditingBatch] = useState(null);
  const [operation, setOperation] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [manufacturers, batches, stocks, references, transactions] = await inventoryApi.loadAll();
      setData({ manufacturers, batches, stocks, references, transactions });
      setRefreshedAt(new Date()); setError(null);
    } catch (caught) { setError(caught); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const lookups = useMemo(() => ({
    manufacturers: Object.fromEntries(data.manufacturers.map((x) => [x.id, x])),
    batches: Object.fromEntries(data.batches.map((x) => [x.id, x])),
    types: Object.fromEntries(data.references.vaccineTypes.map((x) => [x.id, x])),
    centers: Object.fromEntries(data.references.centers.map((x) => [x.id, x])),
    stocks: Object.fromEntries(data.stocks.map((x) => [x.id, x])),
  }), [data]);

  const inventory = useMemo(() => data.stocks.map((stock) => {
    const batchItem = lookups.batches[stock.batchId] || {};
    const remaining = batchItem.expiryDate ? daysUntil(batchItem.expiryDate) : null;
    const effectiveStatus = remaining < 0 ? 'EXPIRED' : batchItem.batchStatus;
    return { ...stock, batch: batchItem, manufacturer: lookups.manufacturers[batchItem.manufacturerId] || {}, type: lookups.types[batchItem.vaccineTypeId] || {}, center: lookups.centers[stock.centerId] || {}, remaining, effectiveStatus,
      nearExpiry: batchItem.batchStatus === 'ACTIVE' && stock.quantity > 0 && remaining >= 0 && remaining <= NEAR_EXPIRY,
      available: effectiveStatus === 'ACTIVE' && stock.quantity > 0 };
  }), [data.stocks, lookups]);

  const lowGroups = useMemo(() => {
    const totals = new Map();
    inventory.filter((x) => x.effectiveStatus === 'ACTIVE').forEach((x) => { const key = `${x.centerId}:${x.batch.vaccineTypeId}`; totals.set(key, (totals.get(key) || 0) + x.quantity); });
    return new Set([...totals].filter(([, amount]) => amount <= LOW_STOCK).map(([key]) => key));
  }, [inventory]);
  const rows = useMemo(() => inventory.map((x) => ({ ...x, lowStock: lowGroups.has(`${x.centerId}:${x.batch.vaccineTypeId}`) })), [inventory, lowGroups]);
  const alerts = useMemo(() => rows.filter((x) => x.lowStock || x.nearExpiry || ['QUARANTINED', 'RECALLED', 'EXPIRED'].includes(x.effectiveStatus)), [rows]);
  const summary = useMemo(() => ({
    doses: rows.filter((x) => x.available).reduce((sum, x) => sum + x.quantity, 0),
    active: data.batches.filter((x) => x.batchStatus === 'ACTIVE' && daysUntil(x.expiryDate) >= 0).length,
    centers: new Set(data.stocks.filter((x) => x.quantity > 0).map((x) => x.centerId)).size,
    low: new Set(rows.filter((x) => x.lowStock).map((x) => `${x.centerId}:${x.batch.vaccineTypeId}`)).size,
    expiry: rows.filter((x) => x.nearExpiry).length,
    restricted: data.batches.filter((x) => ['QUARANTINED', 'RECALLED', 'EXPIRED'].includes(daysUntil(x.expiryDate) < 0 ? 'EXPIRED' : x.batchStatus)).length,
  }), [data, rows]);

  const filtered = useMemo(() => rows.filter((x) => {
    const text = `${x.center.name} ${x.type.name} ${x.manufacturer.name} ${x.batch.batchNumber}`.toLowerCase();
    return (!filters.search || text.includes(filters.search.toLowerCase())) && (!filters.center || String(x.centerId) === filters.center)
      && (!filters.type || String(x.batch.vaccineTypeId) === filters.type) && (!filters.manufacturer || String(x.batch.manufacturerId) === filters.manufacturer)
      && (!filters.status || x.effectiveStatus === filters.status) && (!filters.low || x.lowStock) && (!filters.expiry || x.nearExpiry);
  }).sort((a, b) => {
    const left = sort.field === 'quantity' ? a.quantity : a.batch.expiryDate;
    const right = sort.field === 'quantity' ? b.quantity : b.batch.expiryDate;
    return (left < right ? -1 : left > right ? 1 : 0) * (sort.direction === 'asc' ? 1 : -1);
  }), [rows, filters, sort]);

  const perform = async (action, message) => { setBusy(true); try { await action(); setSuccess(message); setError(null); await load(); return true; } catch (caught) { setError(caught); return false; } finally { setBusy(false); } };
  const submitManufacturer = async (event) => { event.preventDefault(); const ok = await perform(() => editingManufacturer ? inventoryApi.update('manufacturers', editingManufacturer, manufacturerForm) : inventoryApi.create('manufacturers', manufacturerForm), `Manufacturer ${editingManufacturer ? 'updated' : 'registered'}.`); if (ok) { setManufacturerForm(blankManufacturer); setEditingManufacturer(null); } };
  const submitBatch = async (event) => { event.preventDefault(); if (batchForm.manufactureDate >= batchForm.expiryDate) { setError('Manufacture date must be before expiry date.'); return; } const body = { ...batchForm, manufacturerId: Number(batchForm.manufacturerId), vaccineTypeId: Number(batchForm.vaccineTypeId) }; const ok = await perform(() => editingBatch ? inventoryApi.update('batches', editingBatch, body) : inventoryApi.create('batches', body), `Batch ${editingBatch ? 'updated' : 'registered'}.`); if (ok) { setBatchForm(blankBatch); setEditingBatch(null); } };
  const submitStock = async (event) => { event.preventDefault(); const body = { centerId: Number(stockForm.centerId), batchId: Number(stockForm.batchId), quantity: Number(stockForm.quantity) }; const ok = await perform(() => inventoryApi.create('stocks', body), 'Center stock created and initial receipt audited.'); if (ok) setStockForm(blankStock); };
  const remove = (resource, item) => { if (window.confirm(`Delete ${item.name || item.batchNumber}? This cannot be undone.`)) perform(() => inventoryApi.remove(resource, item.id), 'Record deleted.'); };
  const changeSort = (field) => setSort((current) => ({ field, direction: current.field === field && current.direction === 'asc' ? 'desc' : 'asc' }));

  if (loading && !refreshedAt) return <LoadingSpinner message="Loading inventory operations…" />;
  return <section className="inventory-dashboard" aria-labelledby="inventory-title">
    <PageHeader id="inventory-title" eyebrow="MF-04 Operations" title="Inventory & Batch Management" subtitle="Monitor vaccine stock, batch safety, and auditable movements across vaccination centers." actions={<><span className="small text-secondary align-self-center" aria-live="polite">{refreshedAt ? `Last refreshed ${refreshedAt.toLocaleTimeString()}` : 'Not refreshed'}</span><button className="btn btn-outline-primary" onClick={load} disabled={loading}><i className="bi bi-arrow-clockwise me-2" />Refresh</button></>} />
    <ApiErrorAlert error={error} onDismiss={() => setError(null)} />{success && <SuccessAlert message={success} onDismiss={() => setSuccess('')} />}
    <div className="row g-3 mb-4"><SummaryCard icon="bi-box-seam" label="Available doses" value={summary.doses} note="Active, unexpired stock"/><SummaryCard icon="bi-shield-check" label="Active batches" value={summary.active} note="Operationally usable" tone="success"/><SummaryCard icon="bi-building" label="Manufacturers" value={data.manufacturers.length} note="Registered suppliers"/><SummaryCard icon="bi-geo-alt" label="Centers with stock" value={summary.centers} note="Positive quantity"/><SummaryCard icon="bi-exclamation-triangle" label="Low-stock groups" value={summary.low} note="At or below 10 doses" tone="warning"/><SummaryCard icon="bi-calendar2-week" label="Near expiry" value={summary.expiry} note="Within 30 days" tone="warning"/><SummaryCard icon="bi-slash-circle" label="Restricted batches" value={summary.restricted} note="Quarantined, recalled or expired" tone="danger"/></div>
    <nav className="nav nav-pills gap-2 mb-4" aria-label="Inventory dashboard sections">{[['overview','Inventory overview'],['batches','Batch management'],['manufacturers','Manufacturers'],['history','Stock history']].map(([key,label]) => <button type="button" key={key} className={`nav-link ${view === key ? 'active' : ''}`} onClick={() => setView(key)}>{label}</button>)}</nav>

    {view === 'overview' && <Overview alerts={alerts} filtered={filtered} data={data} lookups={lookups} filters={filters} setFilters={setFilters} changeSort={changeSort} setOperation={setOperation} setView={setView} stockForm={stockForm} setStockForm={setStockForm} submitStock={submitStock} busy={busy} />}
    {view === 'manufacturers' && <Manufacturers data={data} form={manufacturerForm} setForm={setManufacturerForm} editing={editingManufacturer} setEditing={setEditingManufacturer} submit={submitManufacturer} remove={remove} busy={busy} />}
    {view === 'batches' && <Batches data={data} lookups={lookups} form={batchForm} setForm={setBatchForm} editing={editingBatch} setEditing={setEditingBatch} submit={submitBatch} remove={remove} busy={busy} />}
    {view === 'history' && <History transactions={data.transactions} lookups={lookups} />}
    {operation && <InventoryOperationModal mode={operation.mode} inventory={operation.row} label={`${operation.row.center.name} · ${operation.row.batch.batchNumber}`} busy={busy} onClose={() => setOperation(null)} onSubmit={async (body) => { const ok = await perform(() => operation.mode === 'receipt' ? inventoryApi.receiveStock(body) : inventoryApi.adjustStock(body), operation.mode === 'receipt' ? 'Stock receipt recorded.' : 'Stock adjustment recorded.'); if (ok) setOperation(null); }} />}
  </section>;
}

function Overview({ alerts, filtered, data, lookups, filters, setFilters, changeSort, setOperation, setView, stockForm, setStockForm, submitStock, busy }) {
  return <><div className="card border-0 shadow-sm mb-4"><div className="card-body"><div className="d-flex justify-content-between mb-3"><div><h2 className="h5 mb-1">Operational alerts</h2><p className="small text-secondary mb-0">Live canonical stock and safety rules.</p></div><span className="badge text-bg-secondary align-self-start">{alerts.length} alerts</span></div>{alerts.length === 0 ? <EmptyState compact icon="bi-check-circle" title="No inventory alerts">Current stock is within configured limits.</EmptyState> : <div className="row g-2">{alerts.slice(0,8).map((row) => { const issue = row.effectiveStatus !== 'ACTIVE' ? title(row.effectiveStatus) : row.nearExpiry ? 'Near expiry' : 'Low stock'; const danger = ['RECALLED','EXPIRED'].includes(row.effectiveStatus); return <div className="col-12 col-lg-6" key={`${row.id}-${issue}`}><div className={`alert inventory-alert mb-0 ${danger ? 'alert-danger' : 'alert-warning'}`}><strong><i className={`bi ${danger ? 'bi-x-octagon' : 'bi-exclamation-triangle'} me-2`}/>{issue}</strong><div className="small">{row.center.name} · {row.batch.batchNumber} · {row.quantity} doses</div></div></div>; })}</div>}</div></div>
    <div className="card border-0 shadow-sm mb-4" id="inventory-table"><div className="card-header bg-white border-0 pt-4 px-4"><h2 className="h5 mb-1">Inventory overview</h2><p className="small text-secondary">Grouped by center, vaccine type, and batch.</p></div><div className="filter-panel border-top border-bottom p-3"><div className="row g-3"><div className="col-12 col-lg-3"><label className="form-label small fw-semibold" htmlFor="inventory-search">Search</label><input id="inventory-search" className="form-control" placeholder="Center, vaccine, maker or batch" value={filters.search} onChange={(e) => setFilters({ ...filters, search: e.target.value })}/></div><div className="col-6 col-lg"><SelectField id="center-filter" label="Centers" all value={filters.center} onChange={(e) => setFilters({ ...filters, center: e.target.value })} options={data.references.centers}/></div><div className="col-6 col-lg"><SelectField id="type-filter" label="Vaccine types" all value={filters.type} onChange={(e) => setFilters({ ...filters, type: e.target.value })} options={data.references.vaccineTypes}/></div><div className="col-6 col-lg"><SelectField id="maker-filter" label="Manufacturers" all value={filters.manufacturer} onChange={(e) => setFilters({ ...filters, manufacturer: e.target.value })} options={data.manufacturers}/></div><div className="col-6 col-lg"><SelectField id="status-filter" label="Statuses" all value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })} options={STATUSES}/></div></div><div className="d-flex gap-3 mt-3"><label><input className="form-check-input me-2" type="checkbox" checked={filters.low} onChange={(e) => setFilters({ ...filters, low: e.target.checked })}/>Low stock only</label><label><input className="form-check-input me-2" type="checkbox" checked={filters.expiry} onChange={(e) => setFilters({ ...filters, expiry: e.target.checked })}/>Near expiry only</label></div></div><div className="table-responsive"><table className="table table-hover align-middle mb-0"><thead className="table-light"><tr><th>Center</th><th>Vaccine type</th><th>Manufacturer</th><th>Batch</th><th><button className="sortable" onClick={() => changeSort('quantity')}>Quantity ↕</button></th><th><button className="sortable" onClick={() => changeSort('expiryDate')}>Expiry ↕</button></th><th>Status</th><th>Availability</th><th>Actions</th></tr></thead><tbody>{filtered.length === 0 ? <tr><td colSpan="9"><EmptyState compact icon="bi-inboxes" title="No matching inventory">Change filters or add center stock.</EmptyState></td></tr> : filtered.map((row) => <tr key={row.id}><td>{row.center.name || `Center #${row.centerId}`}</td><td>{row.type.name || 'Unknown'}</td><td>{row.manufacturer.name || 'Unknown'}</td><td><strong>{row.batch.batchNumber}</strong></td><td>{row.quantity} {row.lowStock && <span className="badge text-bg-warning"><i className="bi bi-exclamation-triangle me-1"/>Low</span>}</td><td>{row.batch.expiryDate}{row.nearExpiry && <small className="d-block text-warning-emphasis"><i className="bi bi-clock me-1"/>{row.remaining} days</small>}</td><td><span className={`badge ${badge(row.effectiveStatus)}`}>{title(row.effectiveStatus)}</span></td><td>{row.available ? <span><i className="bi bi-check-circle-fill text-success me-1"/>Available</span> : <span><i className="bi bi-x-circle-fill text-danger me-1"/>Unavailable</span>}</td><td className="text-nowrap"><button className="btn btn-sm btn-outline-primary me-1" disabled={!row.available} onClick={() => setOperation({ mode:'receipt',row })}>Receipt</button><button className="btn btn-sm btn-outline-secondary" disabled={row.effectiveStatus === 'EXPIRED'} onClick={() => setOperation({ mode:'adjustment',row })}>Adjust</button></td></tr>)}</tbody></table></div></div>
    <div className="card border-0 shadow-sm"><div className="card-body"><h2 className="h5">Quick actions</h2><div className="row g-2"><div className="col"><button className="btn btn-outline-primary w-100 quick-action" onClick={() => setView('manufacturers')}>Register manufacturer</button></div><div className="col"><button className="btn btn-outline-primary w-100 quick-action" onClick={() => setView('batches')}>Register batch</button></div><div className="col"><button className="btn btn-outline-primary w-100 quick-action" onClick={() => setView('history')}>View history</button></div><div className="col"><button className="btn btn-outline-secondary w-100 quick-action" disabled title="Atomic transfer backend is not implemented">Transfer unavailable</button></div></div><form className="row g-3 mt-3 border-top pt-3" onSubmit={submitStock}><h3 className="h6 mb-0">Add center stock</h3><div className="col-md-4"><SelectField id="stock-center" label="Center" value={stockForm.centerId} onChange={(e) => setStockForm({ ...stockForm, centerId:e.target.value })} options={data.references.centers}/></div><div className="col-md-4"><SelectField id="stock-batch" label="Batch" value={stockForm.batchId} onChange={(e) => setStockForm({ ...stockForm, batchId:e.target.value })} options={data.batches.map((x) => ({ id:x.id, code:x.batchNumber, name:lookups.types[x.vaccineTypeId]?.name || 'Vaccine' }))}/></div><div className="col-md-2"><label className="form-label small fw-semibold" htmlFor="stock-quantity">Initial receipt</label><input id="stock-quantity" type="number" min="0" className="form-control" value={stockForm.quantity} onChange={(e) => setStockForm({ ...stockForm, quantity:e.target.value })} required/></div><div className="col-md-2 d-flex align-items-end"><button className="btn btn-primary w-100" disabled={busy}>Add</button></div></form></div></div></>;
}

function Manufacturers({ data, form, setForm, editing, setEditing, submit, remove, busy }) {
  return <div className="row g-4"><div className="col-lg-4"><div className="card border-0 shadow-sm"><div className="card-body"><h2 className="h5">{editing ? 'Edit' : 'Register'} manufacturer</h2><form className="vstack gap-3" onSubmit={submit}><label>Code<input className="form-control" maxLength="64" value={form.code} onChange={(e) => setForm({ ...form, code:e.target.value })} required/></label><label>Name<input className="form-control" maxLength="128" value={form.name} onChange={(e) => setForm({ ...form, name:e.target.value })} required/></label><button className="btn btn-primary" disabled={busy}>{editing ? 'Save changes' : 'Register manufacturer'}</button></form></div></div></div><div className="col-lg-8"><div className="card border-0 shadow-sm"><table className="table table-hover mb-0"><thead><tr><th>Code</th><th>Name</th><th>Actions</th></tr></thead><tbody>{data.manufacturers.map((item) => <tr key={item.id}><td>{item.code}</td><td>{item.name}</td><td><button className="btn btn-sm btn-outline-primary me-2" onClick={() => { setEditing(item.id); setForm({code:item.code,name:item.name}); }}>Edit</button><button className="btn btn-sm btn-outline-danger" onClick={() => remove('manufacturers',item)}>Delete</button></td></tr>)}</tbody></table></div></div></div>;
}

function Batches({ data, lookups, form, setForm, editing, setEditing, submit, remove, busy }) {
  return <div className="row g-4"><div className="col-xl-4"><div className="card border-0 shadow-sm"><div className="card-body"><h2 className="h5">{editing ? 'Edit' : 'Register'} batch</h2><form className="vstack gap-3" onSubmit={submit}><SelectField id="batch-maker" label="Manufacturer" value={form.manufacturerId} onChange={(e) => setForm({...form,manufacturerId:e.target.value})} options={data.manufacturers} disabled={Boolean(editing)}/><SelectField id="batch-type" label="Vaccine type" value={form.vaccineTypeId} onChange={(e) => setForm({...form,vaccineTypeId:e.target.value})} options={data.references.vaccineTypes} disabled={Boolean(editing)}/><label>Batch number<input className="form-control" value={form.batchNumber} onChange={(e) => setForm({...form,batchNumber:e.target.value})} required/></label><label>Manufacture date<input type="date" className="form-control" value={form.manufactureDate} onChange={(e) => setForm({...form,manufactureDate:e.target.value})} required/></label><label>Expiry date<input type="date" min={form.manufactureDate || undefined} className="form-control" value={form.expiryDate} onChange={(e) => setForm({...form,expiryDate:e.target.value})} required/></label><SelectField id="batch-status" label="Status" value={form.batchStatus} onChange={(e) => setForm({...form,batchStatus:e.target.value})} options={STATUSES}/><button className="btn btn-primary" disabled={busy}>{editing ? 'Save batch' : 'Register batch'}</button></form></div></div></div><div className="col-xl-8"><div className="card border-0 shadow-sm"><div className="table-responsive"><table className="table table-hover mb-0"><thead><tr><th>Batch</th><th>Vaccine</th><th>Manufacturer</th><th>Expiry</th><th>Status</th><th>Actions</th></tr></thead><tbody>{data.batches.map((item) => { const effective=daysUntil(item.expiryDate)<0?'EXPIRED':item.batchStatus; return <tr key={item.id}><td><strong>{item.batchNumber}</strong></td><td>{lookups.types[item.vaccineTypeId]?.name}</td><td>{lookups.manufacturers[item.manufacturerId]?.name}</td><td>{item.expiryDate}</td><td><span className={`badge ${badge(effective)}`}>{title(effective)}</span></td><td><button className="btn btn-sm btn-outline-primary me-2" onClick={() => {setEditing(item.id);setForm({...item,manufacturerId:String(item.manufacturerId),vaccineTypeId:String(item.vaccineTypeId)});}}>Edit</button><button className="btn btn-sm btn-outline-danger" onClick={() => remove('batches',item)}>Delete</button></td></tr>;})}</tbody></table></div></div></div></div>;
}

function History({ transactions, lookups }) {
  return <div className="card border-0 shadow-sm"><div className="card-header bg-white border-0 pt-4 px-4"><h2 className="h5">Recent stock transactions</h2><p className="small text-secondary">Read-only append-only audit history; latest 100 entries.</p></div><div className="table-responsive"><table className="table table-hover mb-0"><thead><tr><th>Time</th><th>Type</th><th>Center</th><th>Batch</th><th>Change</th><th>Before → After</th><th>Actor</th><th>Reason</th></tr></thead><tbody>{transactions.length===0?<tr><td colSpan="8"><EmptyState compact icon="bi-clock-history" title="No stock history">Receipts and adjustments appear here.</EmptyState></td></tr>:transactions.map((item)=>{const stock=lookups.stocks[item.inventoryId]||{};return <tr key={item.id}><td>{new Date(item.createdAt).toLocaleString()}</td><td><span className="badge text-bg-light border"><i className="bi bi-journal-text me-1"/>{title(item.type)}</span></td><td>{lookups.centers[stock.centerId]?.name||`Inventory #${item.inventoryId}`}</td><td>{lookups.batches[stock.batchId]?.batchNumber||'—'}</td><td className={item.quantityChange>0?'text-success fw-bold':'text-danger fw-bold'}>{item.quantityChange>0?'+':''}{item.quantityChange}</td><td>{item.beforeQuantity} → {item.afterQuantity}</td><td>{item.actorId?`User #${item.actorId}`:'System'}</td><td>{item.reason||'—'}</td></tr>;})}</tbody></table></div></div>;
}
