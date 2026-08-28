#!/usr/bin/env bash
# =============================================================================
# keycloak-provision.sh
# Provisions the 'cms' realm in Keycloak via Admin REST API.
# Run from the project root AFTER Keycloak is healthy:
#   bash docker/keycloak-provision.sh
#
# Idempotent: safe to re-run — existing resources are skipped (409 = ok).
# =============================================================================
set -euo pipefail

KC_URL="${KEYCLOAK_URL:-http://localhost:8080}"
ADMIN_USER="${KC_ADMIN:-admin}"
ADMIN_PASS="${KC_ADMIN_PASSWORD:-admin123}"
REALM="cms"
CLIENT_ID="cms-backend"
TEST_USER_EMAIL="testuser@cms.com"
TEST_USER_PASS="Test@1234"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
skip()    { echo -e "${YELLOW}[SKIP]${NC}  $*"; }
section() { echo -e "\n${GREEN}══ $* ══${NC}"; }

# Treat 409 (Conflict / already exists) as success
http_post() {
  local url="$1"; shift
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$url" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    "$@")
  if [[ "$status" == "201" || "$status" == "204" || "$status" == "200" ]]; then
    echo "ok"
  elif [[ "$status" == "409" ]]; then
    echo "exists"
  else
    echo "error:$status"
  fi
}

# ─── Step 1: Authenticate to master realm ────────────────────────────────────
section "Step 1: Authenticate to master realm"
RESPONSE=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS")
TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
if [[ -z "$TOKEN" ]]; then
  echo -e "${RED}[ERROR]${NC} Failed to get admin token. Is Keycloak healthy?"
  echo "Response: $RESPONSE"
  exit 1
fi
info "Admin token acquired."

# ─── Step 2: Create realm ────────────────────────────────────────────────────
section "Step 2: Create realm '$REALM'"
result=$(http_post "$KC_URL/admin/realms" \
  -d "{\"realm\":\"$REALM\",\"enabled\":true,\"displayName\":\"CMS\",
       \"registrationAllowed\":false,\"loginWithEmailAllowed\":true,
       \"accessTokenLifespan\":3600,\"refreshTokenMaxReuse\":0}")
[[ "$result" == "exists" ]] && skip "Realm '$REALM' already exists" || info "Realm '$REALM' created."

# Refresh token (realm creation can invalidate master token in some KC versions)
RESPONSE=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=admin-cli&username=$ADMIN_USER&password=$ADMIN_PASS")
TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

# ─── Step 3: Create roles ────────────────────────────────────────────────────
section "Step 3: Create realm roles"
for ROLE in admin account_manager support_agent client; do
  DESCRIPTION=""
  case "$ROLE" in
    admin)           DESCRIPTION="Full system access — user management, config, all data" ;;
    account_manager) DESCRIPTION="Manages client accounts, contracts, invoices" ;;
    support_agent)   DESCRIPTION="Handles support tickets, views client data" ;;
    client)          DESCRIPTION="End-user client portal access" ;;
  esac
  result=$(http_post "$KC_URL/admin/realms/$REALM/roles" \
    -d "{\"name\":\"$ROLE\",\"description\":\"$DESCRIPTION\"}")
  [[ "$result" == "exists" ]] && skip "Role '$ROLE' already exists" || info "Role '$ROLE' created."
done

# ─── Step 4: Create confidential client ─────────────────────────────────────
section "Step 4: Create client '$CLIENT_ID'"
result=$(http_post "$KC_URL/admin/realms/$REALM/clients" \
  -d "{
    \"clientId\": \"$CLIENT_ID\",
    \"name\": \"CMS Backend\",
    \"enabled\": true,
    \"publicClient\": false,
    \"secret\": \"cms-backend-secret-dev\",
    \"standardFlowEnabled\": false,
    \"directAccessGrantsEnabled\": true,
    \"serviceAccountsEnabled\": true,
    \"authorizationServicesEnabled\": false,
    \"redirectUris\": [\"http://localhost:*\"],
    \"webOrigins\": [\"http://localhost:*\"]
  }")
[[ "$result" == "exists" ]] && skip "Client '$CLIENT_ID' already exists" || info "Client '$CLIENT_ID' created (secret: cms-backend-secret-dev)."

section "Step 4.5: Create public client 'cms-admin'"
result=$(http_post "$KC_URL/admin/realms/$REALM/clients" \
  -d "{
    \"clientId\": \"cms-admin\",
    \"name\": \"CMS Admin Frontend\",
    \"enabled\": true,
    \"publicClient\": true,
    \"standardFlowEnabled\": true,
    \"directAccessGrantsEnabled\": false,
    \"serviceAccountsEnabled\": false,
    \"redirectUris\": [\"http://localhost:*\", \"http://127.0.0.1:*\"],
    \"webOrigins\": [\"+\"]
  }")
[[ "$result" == "exists" ]] && skip "Client 'cms-admin' already exists" || info "Client 'cms-admin' created."

# ─── Step 5: Create test user ────────────────────────────────────────────────
section "Step 5: Create test user '$TEST_USER_EMAIL'"
result=$(http_post "$KC_URL/admin/realms/$REALM/users" \
  -d "{
    \"username\": \"testuser\",
    \"email\": \"$TEST_USER_EMAIL\",
    \"firstName\": \"Test\",
    \"lastName\": \"User\",
    \"enabled\": true,
    \"emailVerified\": true,
    \"credentials\": [{\"type\":\"password\",\"value\":\"$TEST_USER_PASS\",\"temporary\":false}]
  }")
[[ "$result" == "exists" ]] && skip "User already exists" || info "User '$TEST_USER_EMAIL' created."

# Get user ID
USER_ID=$(curl -s "$KC_URL/admin/realms/$REALM/users?username=testuser" \
  -H "Authorization: Bearer $TOKEN" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
info "User ID: $USER_ID"

# Create a second test user with only account_manager role (for 403 test)
result=$(http_post "$KC_URL/admin/realms/$REALM/users" \
  -d "{
    \"username\": \"acctmgr\",
    \"email\": \"acctmgr@cms.com\",
    \"firstName\": \"Account\",
    \"lastName\": \"Manager\",
    \"enabled\": true,
    \"emailVerified\": true,
    \"credentials\": [{\"type\":\"password\",\"value\":\"$TEST_USER_PASS\",\"temporary\":false}]
  }")
[[ "$result" == "exists" ]] && skip "Account Manager user already exists" || info "User 'acctmgr@cms.com' created."
ACCTMGR_ID=$(curl -s "$KC_URL/admin/realms/$REALM/users?username=acctmgr" \
  -H "Authorization: Bearer $TOKEN" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

# ─── Step 6: Assign roles ────────────────────────────────────────────────────
section "Step 6: Assign roles to test users"

# Get role representations
get_role() {
  curl -s "$KC_URL/admin/realms/$REALM/roles/$1" \
    -H "Authorization: Bearer $TOKEN"
}

ROLE_ADMIN=$(get_role "admin")
ROLE_SUPPORT=$(get_role "support_agent")
ROLE_ACCT_MGR=$(get_role "account_manager")

# testuser gets: admin + support_agent (multi-role test)
curl -s -X POST "$KC_URL/admin/realms/$REALM/users/$USER_ID/role-mappings/realm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE_ADMIN, $ROLE_SUPPORT]" > /dev/null
info "Assigned [admin, support_agent] to testuser@cms.com"

# acctmgr gets: account_manager only (for 403 test on admin-only endpoint)
curl -s -X POST "$KC_URL/admin/realms/$REALM/users/$ACCTMGR_ID/role-mappings/realm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE_ACCT_MGR]" > /dev/null
info "Assigned [account_manager] to acctmgr@cms.com"

# ─── Summary ─────────────────────────────────────────────────────────────────
section "Provisioning Complete"
echo ""
echo "  Realm:         $KC_URL/realms/$REALM"
echo "  Admin console: $KC_URL/admin/master/console/#/$REALM"
echo ""
echo "  Clients:"
echo "    cms-backend  (secret: cms-backend-secret-dev, direct access grants)"
echo ""
echo "  Test users:"
echo "    testuser@cms.com / $TEST_USER_PASS   → roles: admin, support_agent"
echo "    acctmgr@cms.com  / $TEST_USER_PASS   → roles: account_manager"
echo ""
echo "  Token endpoint:"
echo "    POST $KC_URL/realms/cms/protocol/openid-connect/token"
echo "    -d 'grant_type=password&client_id=cms-backend&client_secret=cms-backend-secret-dev'"
echo "    -d 'username=testuser@cms.com&password=$TEST_USER_PASS'"
echo ""
