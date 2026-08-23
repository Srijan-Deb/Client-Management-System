import React from 'react';
import { useAuth } from './AuthProvider';

interface RoleGateProps {
  allowedRoles: string[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export const useHasRole = (allowedRoles: string[]) => {
  const { roles } = useAuth();
  return allowedRoles.some((role) => roles.includes(role));
};

export const RoleGate = ({ allowedRoles, children, fallback = null }: RoleGateProps) => {
  const hasRole = useHasRole(allowedRoles);

  if (!hasRole) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};
