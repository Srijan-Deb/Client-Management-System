import api from './axios';
import type { Account } from '../types/account';
import type { Page } from '../types/client';

// Gateway: /accounts/** → account-service; account-service path is /api/v1/accounts
const BASE = '/accounts/api/v1/accounts';

export const accountApi = {
  list: (search?: string, page = 0, size = 20): Promise<Page<Account>> =>
    api
      .get(BASE, { params: { search: search || undefined, page, size } })
      .then((r) => r.data),

  get: (id: number): Promise<Account> =>
    api.get(`${BASE}/${id}`).then((r) => r.data),
};
