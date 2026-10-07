import { useCallback, useEffect, useMemo, useState } from "react";
import { inventoryApi } from "../api/inventoryApi";
import { daysUntil } from "../utils/inventoryDates";

const LOW_STOCK = 10;

const NEAR_EXPIRY = 30;

const EMPTY = { manufacturers: [], batches: [], stocks: [], transactions: [], references: { vaccineTypes: [], centers: [] } };

const blankManufacturer = { code: '', name: '' };

const blankBatch = { manufacturerId: '', vaccineTypeId: '', batchNumber: '', manufactureDate: '', expiryDate: '', batchStatus: 'ACTIVE' };

const blankStock = { centerId: '', batchId: '', quantity: 0 };

export default function useInventoryManagement() {
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


  return {
    data,
    view,
    setView,
    loading,
    error,
    setError,
    success,
    setSuccess,
    refreshedAt,
    filters,
    setFilters,
    manufacturerForm,
    setManufacturerForm,
    batchForm,
    setBatchForm,
    stockForm,
    setStockForm,
    editingManufacturer,
    setEditingManufacturer,
    editingBatch,
    setEditingBatch,
    operation,
    setOperation,
    busy,
    load,
    lookups,
    alerts,
    summary,
    filtered,
    perform,
    submitManufacturer,
    submitBatch,
    submitStock,
    remove,
    changeSort
  };
}
