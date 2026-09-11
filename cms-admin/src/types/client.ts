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

export type ContactType = 'PRIMARY' | 'SECONDARY' | 'BILLING' | 'TECHNICAL';
export type AddressType = 'BILLING' | 'SHIPPING' | 'BRANCH' | 'REGISTERED';

export interface ContactResponse {
  contactId: number;
  contactType?: ContactType;
  firstName?: string;
  lastName?: string;
  name?: string;
  email: string;
  phone?: string;
  role?: string;
}

export interface ContactRequest {
  contactType?: ContactType;
  firstName?: string;
  lastName?: string;
  name?: string;
  email: string;
  phone?: string;
  role?: string;
}

export interface AddressResponse {
  addressId: number;
  addressType?: AddressType;
  line1?: string;
  line2?: string;
  street?: string;
  city: string;
  state?: string;
  country: string;
  postalCode?: string;
  primary?: boolean;
}

export interface AddressRequest {
  addressType?: AddressType;
  line1?: string;
  line2?: string;
  street?: string;
  city: string;
  state?: string;
  country: string;
  postalCode?: string;
  primary?: boolean;
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
