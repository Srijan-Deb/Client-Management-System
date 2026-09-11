import { evaluatePasswordStrength } from '../utils/passwordValidation';

interface PasswordStrengthMeterProps {
  password: string;
  showRules?: boolean;
}

export const PasswordStrengthMeter = ({
  password,
  showRules = true,
}: PasswordStrengthMeterProps) => {
  const strength = evaluatePasswordStrength(password);

  if (!password) {
    return null;
  }

  return (
    <div style={{ marginTop: '8px', marginBottom: '14px' }}>
      {/* Header bar: Label & Score */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '6px',
        }}
      >
        <span style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-secondary)' }}>
          Password Strength:
        </span>
        <span
          style={{
            fontSize: '11px',
            fontWeight: 700,
            color: strength.color,
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
          }}
        >
          {strength.label}
        </span>
      </div>

      {/* 4-bar Progress Indicator */}
      <div style={{ display: 'flex', gap: '4px', height: '4px', marginBottom: '8px' }}>
        {[1, 2, 3, 4].map((step) => {
          const isActive = strength.score >= step;
          return (
            <div
              key={step}
              style={{
                flex: 1,
                borderRadius: '2px',
                background: isActive ? strength.color : 'var(--border)',
                transition: 'background 0.25s ease',
              }}
            />
          );
        })}
      </div>

      {/* Checklist of rules */}
      {showRules && (
        <div
          style={{
            background: 'var(--bg-elevated)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius-sm)',
            padding: '10px 12px',
            fontSize: '11px',
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '6px',
          }}
        >
          {Object.entries(strength.rules).map(([key, rule]) => (
            <div
              key={key}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                color: rule.met ? 'var(--success)' : 'var(--text-muted)',
                transition: 'color 0.2s ease',
              }}
            >
              <span
                style={{
                  fontSize: '12px',
                  fontWeight: 700,
                }}
              >
                {rule.met ? '✓' : '•'}
              </span>
              <span style={{ textDecoration: rule.met ? 'none' : 'none' }}>
                {rule.label}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
