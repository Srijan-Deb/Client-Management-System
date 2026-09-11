// ─── Country definitions and Phone Validation Rules ────────────────────────

export interface CountryInfo {
  code: string;       // ISO 3166-1 alpha-2
  name: string;
  dialCode: string;   // e.g. "+91"
  flag: string;       // emoji flag
  placeholder: string;
  minDigits: number;
  maxDigits: number;
  validator?: (nationalNumber: string) => { isValid: boolean; error?: string };
}

export const COUNTRIES: CountryInfo[] = [
  {
    code: 'IN',
    name: 'India',
    dialCode: '+91',
    flag: '🇮🇳',
    placeholder: '98765 43210',
    minDigits: 10,
    maxDigits: 10,
    validator: (num) => {
      if (num.length !== 10) {
        return { isValid: false, error: 'Indian phone numbers must be exactly 10 digits.' };
      }
      if (!/^[6-9]/.test(num)) {
        return { isValid: false, error: 'Indian mobile numbers must start with 6, 7, 8, or 9.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'US',
    name: 'United States',
    dialCode: '+1',
    flag: '🇺🇸',
    placeholder: '(555) 234-5678',
    minDigits: 10,
    maxDigits: 10,
    validator: (num) => {
      if (num.length !== 10) {
        return { isValid: false, error: 'US phone numbers must be 10 digits.' };
      }
      if (/^[01]/.test(num)) {
        return { isValid: false, error: 'US area code cannot start with 0 or 1.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'GB',
    name: 'United Kingdom',
    dialCode: '+44',
    flag: '🇬🇧',
    placeholder: '7911 123456',
    minDigits: 10,
    maxDigits: 10,
    validator: (num) => {
      if (num.length < 10 || num.length > 11) {
        return { isValid: false, error: 'UK phone numbers must be 10 or 11 digits.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'CA',
    name: 'Canada',
    dialCode: '+1',
    flag: '🇨🇦',
    placeholder: '(416) 234-5678',
    minDigits: 10,
    maxDigits: 10,
    validator: (num) => {
      if (num.length !== 10) {
        return { isValid: false, error: 'Canadian phone numbers must be 10 digits.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'AU',
    name: 'Australia',
    dialCode: '+61',
    flag: '🇦🇺',
    placeholder: '412 345 678',
    minDigits: 9,
    maxDigits: 9,
    validator: (num) => {
      if (num.length !== 9) {
        return { isValid: false, error: 'Australian phone numbers must be 9 digits (excluding leading 0).' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'DE',
    name: 'Germany',
    dialCode: '+49',
    flag: '🇩🇪',
    placeholder: '151 23456789',
    minDigits: 10,
    maxDigits: 11,
    validator: (num) => {
      if (num.length < 10 || num.length > 11) {
        return { isValid: false, error: 'German phone numbers must be 10 or 11 digits.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'AE',
    name: 'United Arab Emirates',
    dialCode: '+971',
    flag: '🇦🇪',
    placeholder: '50 123 4567',
    minDigits: 9,
    maxDigits: 9,
    validator: (num) => {
      if (num.length !== 9) {
        return { isValid: false, error: 'UAE phone numbers must be 9 digits.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'SG',
    name: 'Singapore',
    dialCode: '+65',
    flag: '🇸🇬',
    placeholder: '9123 4567',
    minDigits: 8,
    maxDigits: 8,
    validator: (num) => {
      if (num.length !== 8) {
        return { isValid: false, error: 'Singapore phone numbers must be 8 digits.' };
      }
      if (!/^[89]/.test(num)) {
        return { isValid: false, error: 'Singapore mobile numbers must start with 8 or 9.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'FR',
    name: 'France',
    dialCode: '+33',
    flag: '🇫🇷',
    placeholder: '6 12 34 56 78',
    minDigits: 9,
    maxDigits: 9,
    validator: (num) => {
      if (num.length !== 9) {
        return { isValid: false, error: 'French phone numbers must be 9 digits (excluding leading 0).' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'JP',
    name: 'Japan',
    dialCode: '+81',
    flag: '🇯🇵',
    placeholder: '90 1234 5678',
    minDigits: 10,
    maxDigits: 10,
    validator: (num) => {
      if (num.length !== 10) {
        return { isValid: false, error: 'Japanese mobile numbers must be 10 digits.' };
      }
      return { isValid: true };
    },
  },
  {
    code: 'OTHER',
    name: 'Other / International',
    dialCode: '+',
    flag: '🌐',
    placeholder: '1234567890',
    minDigits: 7,
    maxDigits: 15,
    validator: (num) => {
      if (num.length < 7 || num.length > 15) {
        return { isValid: false, error: 'International numbers must be between 7 and 15 digits.' };
      }
      return { isValid: true };
    },
  },
];

export const DEFAULT_COUNTRY = COUNTRIES[0]; // India (+91)

/**
 * Validates a national phone number according to the selected country's rules.
 */
export function validatePhoneNumber(
  rawNumber: string,
  countryCode: string
): { isValid: boolean; error?: string; formattedE164: string } {
  // Strip non-digit characters
  const cleanDigits = rawNumber.replace(/\D/g, '');
  const country = COUNTRIES.find((c) => c.code === countryCode) || DEFAULT_COUNTRY;

  if (!cleanDigits) {
    return { isValid: true, formattedE164: '' };
  }

  // Check digit length limits
  if (cleanDigits.length < country.minDigits) {
    return {
      isValid: false,
      error: `Too short for ${country.name} (minimum ${country.minDigits} digits).`,
      formattedE164: `${country.dialCode}${cleanDigits}`,
    };
  }

  if (cleanDigits.length > country.maxDigits) {
    return {
      isValid: false,
      error: `Too long for ${country.name} (maximum ${country.maxDigits} digits).`,
      formattedE164: `${country.dialCode}${cleanDigits}`,
    };
  }

  // Country-specific validator if available
  if (country.validator) {
    const res = country.validator(cleanDigits);
    if (!res.isValid) {
      return {
        isValid: false,
        error: res.error,
        formattedE164: `${country.dialCode}${cleanDigits}`,
      };
    }
  }

  return {
    isValid: true,
    formattedE164: `${country.dialCode}${cleanDigits}`,
  };
}

/**
 * Parses an existing E.164 phone string (e.g. "+919876543210")
 * into country code and local national digits.
 */
export function parseStoredPhone(phone?: string): { countryCode: string; nationalNumber: string } {
  if (!phone) {
    return { countryCode: DEFAULT_COUNTRY.code, nationalNumber: '' };
  }

  const trimmed = phone.trim();
  for (const c of COUNTRIES) {
    if (c.code !== 'OTHER' && trimmed.startsWith(c.dialCode)) {
      const national = trimmed.slice(c.dialCode.length).replace(/\D/g, '');
      return { countryCode: c.code, nationalNumber: national };
    }
  }

  // Fallback to only digits or other
  const digits = trimmed.replace(/\D/g, '');
  return { countryCode: DEFAULT_COUNTRY.code, nationalNumber: digits };
}
