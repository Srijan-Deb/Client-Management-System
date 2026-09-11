// ─── Account types matching AccountResponse DTO ───────────────────────────

import type { Page } from './client';

export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface Account {
  accountId: number;
  accountName: string;
  email: string;
  status: AccountStatus;
  createdAt: string;
}

export type { Page };
