import { createContext, useContext, useState, useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
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
  const initCalled = useRef(false); // Guard against React 19 double-effect in StrictMode

  useEffect(() => {
    if (initCalled.current) return;
    initCalled.current = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256', // Security best practice for public clients
      })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        setIsInitialized(true);

        if (authenticated) {
          setToken(keycloak.token ?? null);
          // Decode realm roles from the token
          setRoles(keycloak.realmAccess?.roles ?? []);
          keycloak.loadUserProfile().then((profile) => setUser(profile));

          // Silent token refresh: refresh token 70s before expiry
          keycloak.onTokenExpired = () => {
            keycloak
              .updateToken(70)
              .then((refreshed) => {
                if (refreshed) {
                  setToken(keycloak.token ?? null);
                  setRoles(keycloak.realmAccess?.roles ?? []);
                }
              })
              .catch(() => {
                console.error('Token refresh failed — forcing re-login');
                keycloak.login();
              });
          };
        }
      })
      .catch((err) => {
        console.error('Keycloak init failed', err);
        setIsInitialized(true); // Still mark as initialized so the UI renders
      });
  }, []);

  const login = () => keycloak.login();
  const logout = () =>
    keycloak.logout({ redirectUri: `${window.location.origin}/` });

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, user, roles, token, login, logout, isInitialized }}
    >
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
