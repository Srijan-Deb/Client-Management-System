import { useState, useEffect } from 'react';
import {
  COUNTRIES,
  DEFAULT_COUNTRY,
  validatePhoneNumber,
  parseStoredPhone,
  type CountryInfo,
} from '../utils/phoneValidation';

interface PhoneInputWithCountryProps {
  value?: string;
  onChange: (formattedE164: string, isValid: boolean) => void;
  label?: string;
  required?: boolean;
  disabled?: boolean;
  id?: string;
  error?: string;
}

export const PhoneInputWithCountry = ({
  value = '',
  onChange,
  label = 'Phone Number',
  required = false,
  disabled = false,
  id = 'phone-input',
  error: externalError,
}: PhoneInputWithCountryProps) => {
  const parsed = parseStoredPhone(value);
  const [selectedCountryCode, setSelectedCountryCode] = useState<string>(parsed.countryCode);
  const [nationalNumber, setNationalNumber] = useState<string>(parsed.nationalNumber);
  const [internalError, setInternalError] = useState<string>('');

  const currentCountry: CountryInfo =
    COUNTRIES.find((c) => c.code === selectedCountryCode) || DEFAULT_COUNTRY;

  // Sync if external value changes (e.g. in Edit modals)
  useEffect(() => {
    if (value) {
      const p = parseStoredPhone(value);
      setSelectedCountryCode(p.countryCode);
      setNationalNumber(p.nationalNumber);
    }
  }, [value]);

  const handleCountryChange = (newCountryCode: string) => {
    setSelectedCountryCode(newCountryCode);
    const country = COUNTRIES.find((c) => c.code === newCountryCode) || DEFAULT_COUNTRY;

    if (!nationalNumber) {
      setInternalError('');
      onChange('', !required);
      return;
    }

    const validation = validatePhoneNumber(nationalNumber, country.code);
    if (!validation.isValid && nationalNumber.length > 0) {
      setInternalError(validation.error || 'Invalid phone format');
      onChange(validation.formattedE164, false);
    } else {
      setInternalError('');
      onChange(validation.formattedE164, true);
    }
  };

  const handleNumberChange = (raw: string) => {
    // Only allow digits, spaces, and hyphens in input
    const cleaned = raw.replace(/[^\d\s-]/g, '');
    const digitsOnly = cleaned.replace(/\D/g, '');
    setNationalNumber(cleaned);

    if (!digitsOnly) {
      setInternalError('');
      onChange('', !required);
      return;
    }

    const validation = validatePhoneNumber(digitsOnly, selectedCountryCode);
    if (!validation.isValid) {
      setInternalError(validation.error || 'Invalid phone format');
      onChange(validation.formattedE164, false);
    } else {
      setInternalError('');
      onChange(validation.formattedE164, true);
    }
  };

  const activeError = externalError || internalError;

  return (
    <div className="form-group" style={{ marginBottom: '14px' }}>
      {label && (
        <label htmlFor={id} className="form-label" style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>
            {label} {required && <span style={{ color: 'var(--danger)' }}>*</span>}
          </span>
          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
            {currentCountry.name} ({currentCountry.dialCode})
          </span>
        </label>
      )}

      <div
        style={{
          display: 'flex',
          gap: '8px',
          alignItems: 'center',
        }}
      >
        {/* Country Selector */}
        <select
          value={selectedCountryCode}
          onChange={(e) => handleCountryChange(e.target.value)}
          disabled={disabled}
          className="form-input"
          style={{
            width: '130px',
            flexShrink: 0,
            padding: '10px 8px',
            fontSize: '13px',
            cursor: 'pointer',
          }}
          aria-label="Select Country"
        >
          {COUNTRIES.map((c) => (
            <option key={c.code} value={c.code}>
              {c.flag} {c.code} ({c.dialCode})
            </option>
          ))}
        </select>

        {/* National Number Input */}
        <div style={{ position: 'relative', width: '100%' }}>
          <span
            style={{
              position: 'absolute',
              left: '12px',
              top: '50%',
              transform: 'translateY(-50%)',
              color: 'var(--text-muted)',
              fontSize: '13px',
              fontWeight: 500,
              pointerEvents: 'none',
            }}
          >
            {currentCountry.dialCode}
          </span>
          <input
            id={id}
            type="tel"
            value={nationalNumber}
            onChange={(e) => handleNumberChange(e.target.value)}
            placeholder={currentCountry.placeholder}
            disabled={disabled}
            className={`form-input ${activeError ? 'form-input-error' : ''}`}
            style={{
              paddingLeft: `${currentCountry.dialCode.length * 9 + 18}px`,
            }}
          />
        </div>
      </div>

      {/* Validation Message */}
      {activeError ? (
        <p className="form-error" style={{ fontSize: '11.5px', marginTop: '5px', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <span>⚠️</span> {activeError}
        </p>
      ) : nationalNumber && !internalError ? (
        <p style={{ fontSize: '11px', color: 'var(--success)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <span>✓</span> Valid {currentCountry.name} format ({currentCountry.dialCode} {nationalNumber.replace(/\D/g, '')})
        </p>
      ) : null}
    </div>
  );
};
