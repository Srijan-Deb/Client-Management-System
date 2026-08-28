import Keycloak from 'keycloak-js';

// Singleton Keycloak instance — must NOT be re-created on React re-renders
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL as string,
  realm: import.meta.env.VITE_KEYCLOAK_REALM as string,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string,
});

export default keycloak;
