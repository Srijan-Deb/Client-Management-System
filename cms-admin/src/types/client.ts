// ─── Client types matching the backend DTOs exactly ───────────────────────

export type ClientTier = 'STANDARD' | 'PREMIUM' | 'ENTERPRISE';
export type ClientStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';

export interface ClientSummary {
  clientId: number;
  accountId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  companyName?: string;
  tier: ClientTier;
  status: ClientStatus;
  createdAt: string;
}

export interface ContactResponse {
  contactId: number;
  name: string;
  email: string;
  phone?: string;
  role?: string;
}

export interface AddressResponse {
  addressId: number;
  street: string;
  city: string;
  state?: string;
  country: string;
  postalCode?: string;
}

export interface Client extends ClientSummary {
  contacts: ContactResponse[];
  addresses: AddressResponse[];
  updatedAt: string;
}

// Spring Page wrapper
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateClientRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  companyName?: string;
  tier: ClientTier;
}

export interface UpdateClientRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  companyName?: string;
  tier?: ClientTier;
  status?: ClientStatus;
}
