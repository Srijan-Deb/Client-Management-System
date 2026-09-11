#!/bin/bash
# =============================================================
# CMS Kafka Topic Initialiser
# Runs as a one-shot Docker service after Kafka is healthy.
# Creates all 5 CMS topics with appropriate settings.
# =============================================================

set -e

KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP:-"kafka:29092"}
REPLICATION_FACTOR=${KAFKA_REPLICATION_FACTOR:-1}  # Default to 1, overridden by cluster compose
PARTITIONS=${KAFKA_PARTITIONS:-3}                  # 3 partitions allows future horizontal consumer scaling

KAFKA_HOST="${KAFKA_BOOTSTRAP%%:*}"
KAFKA_PORT="${KAFKA_BOOTSTRAP##*:}"

echo "==> Waiting for Kafka TCP socket at ${KAFKA_HOST}:${KAFKA_PORT}..."
while ! bash -c "</dev/tcp/${KAFKA_HOST}/${KAFKA_PORT}" 2>/dev/null; do
  echo "    Kafka not reachable yet, waiting 3s..."
  sleep 3
done
echo "    TCP socket is open."

echo "==> Verifying Kafka broker accepts protocol requests..."
MAX_RETRIES=30
RETRY=0
while ! kafka-topics --bootstrap-server "${KAFKA_BOOTSTRAP}" --list >/dev/null 2>&1; do
  RETRY=$((RETRY + 1))
  if [ $RETRY -ge $MAX_RETRIES ]; then
    echo "    ERROR: Kafka broker did not become ready after ${MAX_RETRIES} retries."
    exit 1
  fi
  echo "    Broker not accepting requests yet, waiting 5s... (attempt ${RETRY}/${MAX_RETRIES})"
  sleep 5
done
echo "==> Kafka broker is ready and accepting API requests!"

echo "==> Fetching existing topics to speed up initialization..."
EXISTING_TOPICS=$(kafka-topics --bootstrap-server "${KAFKA_BOOTSTRAP}" --list || echo "")

create_topic() {
  local TOPIC=$1
  local PARTS=${2:-$PARTITIONS}
  if echo "$EXISTING_TOPICS" | grep -q "^${TOPIC}$"; then
    echo "  Topic already exists, skipping: ${TOPIC}"
    return 0
  fi
  echo "  Creating topic: ${TOPIC} (partitions=${PARTS})"
  kafka-topics \
    --bootstrap-server "${KAFKA_BOOTSTRAP}" \
    --create \
    --if-not-exists \
    --topic "${TOPIC}" \
    --partitions "${PARTS}" \
    --replication-factor "${REPLICATION_FACTOR}" \
    --command-config <(echo "request.timeout.ms=120000")
}

# CMS Kafka topics (matches ClientOnboardedEvent, InvoiceGeneratedEvent, etc.)
create_topic "client-onboarded"
create_topic "invoice-generated"
create_topic "payment-success"
create_topic "payment-failed"
create_topic "ticket-created"
create_topic "subscription-created"

# Dead-letter topics — required because KAFKA_AUTO_CREATE_TOPICS_ENABLE=false.
# DefaultErrorHandler publishes to {topic}.DLT after retries exhaust;
# without these topics DeadLetterPublishingRecoverer would fail and the message
# would be lost. 1 partition is intentional: DLT consumption is single-threaded.
echo "  Creating Dead-Letter Topics (*.DLT)..."
for MAIN_TOPIC in client-onboarded invoice-generated payment-success payment-failed ticket-created subscription-created; do
  create_topic "${MAIN_TOPIC}.DLT" 1
done

echo ""
echo "==> All CMS Kafka topics and DLT topics created. Listing:"
kafka-topics --bootstrap-server "${KAFKA_BOOTSTRAP}" --list

echo "==> kafka-init done."
