import { apiRequestData } from '../../../services/apiClient';

const json = (method, body) => ({ method, body: JSON.stringify(body) });
const resource = (name) => `/v1/inventory/${name}`;

export const inventoryApi = {
  loadAll: () => Promise.all([
    apiRequestData(resource('manufacturers')),
    apiRequestData(resource('batches')),
    apiRequestData(resource('stocks')),
    apiRequestData(resource('reference-data')),
  ]),
  create: (name, body) => apiRequestData(resource(name), json('POST', body)),
  update: (name, id, body) => apiRequestData(`${resource(name)}/${id}`, json('PUT', body)),
  remove: (name, id) => apiRequestData(`${resource(name)}/${id}`, { method: 'DELETE' }),
};
