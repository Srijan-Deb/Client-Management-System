import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import keycloak from '../lib/keycloak';

interface AuthContextType {
  isAuthenticated: boolean;
  user: any;
  roles: string[];
  token: string | null;
  login: () => void;
  logout: () => void;
  isInitialized: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<any>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    keycloak
      .init({ onLoad: 'check-sso', silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html' })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        setIsInitialized(true);
        if (authenticated) {
          setToken(keycloak.token || null);
          setRoles(keycloak.realmAccess?.roles || []);
          keycloak.loadUserProfile().then((profile) => setUser(profile));

          // Set up token refresh timer
          setInterval(() => {
            keycloak.updateToken(70).then((refreshed) => {
              if (refreshed) {
                setToken(keycloak.token || null);
                setRoles(keycloak.realmAccess?.roles || []);
              }
            }).catch(() => {
              console.error('Failed to refresh token');
              keycloak.logout();
            });
          }, 60000); // Check every minute
        }
      })
      .catch(console.error);
  }, []);

  const login = () => keycloak.login();
  const logout = () => keycloak.logout();

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, roles, token, login, logout, isInitialized }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
