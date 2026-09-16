import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { inventoryApi } from '../api/inventoryApi';

const emptyManufacturer = { code: '', name: '' };
const emptyBatch = { manufacturerId: '', vaccineTypeId: '', batchNumber: '', manufactureDate: '', expiryDate: '', batchStatus: 'ACTIVE' };
const emptyStock = { centerId: '', batchId: '', quantity: 0 };
const sections = { manufacturers: emptyManufacturer, batches: emptyBatch, stocks: emptyStock };

function Select({ value, onChange, options, label }) {
  return <select className="form-select" aria-label={label} value={value} onChange={onChange} required>
    <option value="">Select {label}</option>
    {options.map((option) => <option key={option.id} value={option.id}>{option.code} — {option.name}</option>)}
  </select>;
}

export default function InventoryManagementPage() {
  const [active, setActive] = useState('manufacturers');
  const [data, setData] = useState({ manufacturers: [], batches: [], stocks: [], references: { vaccineTypes: [], centers: [] } });
  const [form, setForm] = useState(emptyManufacturer);
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [manufacturers, batches, stocks, references] = await inventoryApi.loadAll();
      setData({ manufacturers, batches, stocks, references });
      setError('');
    } catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const names = useMemo(() => ({
    manufacturer: Object.fromEntries(data.manufacturers.map((x) => [x.id, x.name])),
    batch: Object.fromEntries(data.batches.map((x) => [x.id, x.batchNumber])),
    type: Object.fromEntries(data.references.vaccineTypes.map((x) => [x.id, x.name])),
    center: Object.fromEntries(data.references.centers.map((x) => [x.id, x.name])),
  }), [data]);

  const switchSection = (section) => { setActive(section); setForm(sections[section]); setEditingId(null); setMessage(''); setError(''); };
  const change = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const normalize = () => active === 'manufacturers' ? form : active === 'batches'
    ? { ...form, manufacturerId: Number(form.manufacturerId), vaccineTypeId: Number(form.vaccineTypeId) }
    : { ...form, centerId: Number(form.centerId), batchId: Number(form.batchId), quantity: Number(form.quantity) };
  const submit = async (event) => {
    event.preventDefault();
    try {
      if (editingId) await inventoryApi.update(active, editingId, normalize());
      else await inventoryApi.create(active, normalize());
      setMessage(`${editingId ? 'Updated' : 'Created'} successfully.`); setError('');
      setEditingId(null); setForm(sections[active]); await load();
    } catch (exception) { setError(exception.message); setMessage(''); }
  };
  const edit = (item) => { setEditingId(item.id); setForm(active === 'manufacturers' ? { code: item.code, name: item.name }
    : active === 'batches' ? { ...item, manufacturerId: String(item.manufacturerId), vaccineTypeId: String(item.vaccineTypeId) }
    : { centerId: String(item.centerId), batchId: String(item.batchId), quantity: item.quantity }); };
  const remove = async (id) => {
    if (!window.confirm('Delete this record?')) return;
    try { await inventoryApi.remove(active, id); setMessage('Deleted successfully.'); await load(); }
    catch (exception) { setError(exception.message); }
  };

  const rows = data[active];
  return <section aria-labelledby="inventory-title">
    <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
      <div><span className="badge text-bg-primary mb-2">MF-04</span><h1 id="inventory-title" className="h2 mb-1">Inventory &amp; Batch Management</h1><p className="text-muted mb-0">Manage manufacturers, vaccine batches, and stock held at vaccination centers.</p></div>
      <button className="btn btn-outline-primary" onClick={load}><i className="bi bi-arrow-clockwise me-2" />Refresh</button>
    </div>
    <ul className="nav nav-pills gap-2 mb-4">{Object.keys(sections).map((section) => <li key={section} className="nav-item"><button className={`nav-link ${active === section ? 'active' : ''}`} onClick={() => switchSection(section)}>{section === 'stocks' ? 'Center Stock' : section[0].toUpperCase() + section.slice(1)}</button></li>)}</ul>
    {error && <div className="alert alert-danger">{error}</div>}{message && <div className="alert alert-success">{message}</div>}
    <div className="row g-4">
      <div className="col-12 col-xl-4"><div className="card border-0 shadow-sm"><div className="card-body"><h2 className="h5 mb-3">{editingId ? 'Update' : 'Create'} record</h2><form onSubmit={submit} className="vstack gap-3">
        {active === 'manufacturers' && <><input className="form-control" placeholder="Manufacturer code" value={form.code} onChange={change('code')} required maxLength="64"/><input className="form-control" placeholder="Manufacturer name" value={form.name} onChange={change('name')} required maxLength="128"/></>}
        {active === 'batches' && <><Select label="manufacturer" value={form.manufacturerId} onChange={change('manufacturerId')} options={data.manufacturers}/><Select label="vaccine type" value={form.vaccineTypeId} onChange={change('vaccineTypeId')} options={data.references.vaccineTypes}/><input className="form-control" placeholder="Batch number" value={form.batchNumber} onChange={change('batchNumber')} required/><label className="form-label mb-0">Manufactured<input type="date" className="form-control mt-1" value={form.manufactureDate} onChange={change('manufactureDate')} required/></label><label className="form-label mb-0">Expires<input type="date" className="form-control mt-1" value={form.expiryDate} onChange={change('expiryDate')} required/></label><select className="form-select" value={form.batchStatus} onChange={change('batchStatus')}>{['ACTIVE','QUARANTINED','RECALLED','EXPIRED','INACTIVE'].map((x) => <option key={x}>{x}</option>)}</select></>}
        {active === 'stocks' && <><Select label="center" value={form.centerId} onChange={change('centerId')} options={data.references.centers}/><Select label="batch" value={form.batchId} onChange={change('batchId')} options={data.batches.map((x) => ({ id: x.id, code: x.batchNumber, name: names.type[x.vaccineTypeId] || 'Vaccine' }))}/><input type="number" min="0" className="form-control" placeholder="Available quantity" value={form.quantity} onChange={change('quantity')} required/></>}
        <div className="d-flex gap-2"><button className="btn btn-primary" type="submit">{editingId ? 'Save changes' : 'Create'}</button>{editingId && <button type="button" className="btn btn-outline-secondary" onClick={() => { setEditingId(null); setForm(sections[active]); }}>Cancel</button>}</div>
      </form></div></div></div>
      <div className="col-12 col-xl-8"><div className="card border-0 shadow-sm"><div className="table-responsive"><table className="table table-hover align-middle mb-0"><thead className="table-light"><tr>{active === 'manufacturers' ? <><th>Code</th><th>Name</th></> : active === 'batches' ? <><th>Batch</th><th>Manufacturer</th><th>Type</th><th>Status / Expiry</th></> : <><th>Center</th><th>Batch</th><th>Available</th></>}<th className="text-end">Actions</th></tr></thead><tbody>
        {loading ? <tr><td colSpan="5" className="text-center py-5">Loading inventory…</td></tr> : rows.length === 0 ? <tr><td colSpan="5" className="text-center text-muted py-5">No records yet. Create your first one.</td></tr> : rows.map((item) => <tr key={item.id}>{active === 'manufacturers' ? <><td>{item.code}</td><td>{item.name}</td></> : active === 'batches' ? <><td>{item.batchNumber}</td><td>{names.manufacturer[item.manufacturerId]}</td><td>{names.type[item.vaccineTypeId]}</td><td><span className="badge text-bg-secondary">{item.batchStatus}</span><div className="small text-muted mt-1">{item.expiryDate}</div></td></> : <><td>{names.center[item.centerId]}</td><td>{names.batch[item.batchId]}</td><td><strong>{item.quantity}</strong> doses</td></>}<td className="text-end text-nowrap"><button className="btn btn-sm btn-outline-primary me-2" onClick={() => edit(item)}>Edit</button><button className="btn btn-sm btn-outline-danger" onClick={() => remove(item.id)}>Delete</button></td></tr>)}
      </tbody></table></div></div></div>
    </div>
  </section>;
}
