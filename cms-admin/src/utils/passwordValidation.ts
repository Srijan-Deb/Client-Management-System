// ─── Strong Password Validation Policy ─────────────────────────────────────

export interface PasswordRuleResult {
  label: string;
  met: boolean;
}

export interface PasswordStrength {
  score: number;             // 0 (empty) to 4 (strong)
  label: 'Empty' | 'Weak' | 'Fair' | 'Good' | 'Strong';
  color: string;
  isStrong: boolean;
  rules: {
    length: PasswordRuleResult;
    hasUpperCase: PasswordRuleResult;
    hasLowerCase: PasswordRuleResult;
    hasNumber: PasswordRuleResult;
    hasSpecialChar: PasswordRuleResult;
  };
}

export function evaluatePasswordStrength(password: string): PasswordStrength {
  const lengthMet = password.length >= 8 && password.length <= 64;
  const upperMet = /[A-Z]/.test(password);
  const lowerMet = /[a-z]/.test(password);
  const numberMet = /[0-9]/.test(password);
  const specialMet = /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~`]/.test(password);

  const rules = {
    length: {
      label: 'At least 8 characters',
      met: lengthMet,
    },
    hasUpperCase: {
      label: 'At least 1 uppercase letter (A-Z)',
      met: upperMet,
    },
    hasLowerCase: {
      label: 'At least 1 lowercase letter (a-z)',
      met: lowerMet,
    },
    hasNumber: {
      label: 'At least 1 number (0-9)',
      met: numberMet,
    },
    hasSpecialChar: {
      label: 'At least 1 special character (!@#$%^&*)',
      met: specialMet,
    },
  };

  if (!password) {
    return {
      score: 0,
      label: 'Empty',
      color: 'var(--text-muted)',
      isStrong: false,
      rules,
    };
  }

  // Count met criteria
  const metCount = [lengthMet, upperMet, lowerMet, numberMet, specialMet].filter(Boolean).length;
  const isStrong = metCount === 5;

  let score = 1;
  let label: PasswordStrength['label'] = 'Weak';
  let color = 'var(--danger)'; // #ef4444

  if (metCount >= 5) {
    score = 4;
    label = 'Strong';
    color = 'var(--success)'; // #10b981
  } else if (metCount === 4) {
    score = 3;
    label = 'Good';
    color = '#3b82f6'; // blue
  } else if (metCount === 3) {
    score = 2;
    label = 'Fair';
    color = 'var(--warning)'; // #f59e0b
  } else {
    score = 1;
    label = 'Weak';
    color = 'var(--danger)';
  }

  return {
    score,
    label,
    color,
    isStrong,
    rules,
  };
}
