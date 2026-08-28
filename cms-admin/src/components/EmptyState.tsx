import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: string;
  title: string;
  description: string;
  action?: ReactNode;
}

export const EmptyState = ({
  icon = '📦',
  title,
  description,
  action,
}: EmptyStateProps) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '64px 24px',
        textAlign: 'center',
        background: 'var(--bg-surface)',
        borderRadius: 'var(--radius)',
        border: '1px dashed var(--border)',
        minHeight: '320px',
      }}
    >
      <div
        style={{
          fontSize: '48px',
          marginBottom: '16px',
          opacity: 0.8,
        }}
      >
        {icon}
      </div>
      <h3
        style={{
          fontSize: '18px',
          fontWeight: 600,
          color: 'var(--text-primary)',
          marginBottom: '8px',
        }}
      >
        {title}
      </h3>
      <p
        style={{
          fontSize: '14px',
          color: 'var(--text-secondary)',
          maxWidth: '400px',
          lineHeight: 1.5,
          marginBottom: action ? '24px' : '0',
        }}
      >
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
};
