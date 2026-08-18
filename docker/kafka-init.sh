#!/bin/bash
# =============================================================
# CMS Kafka Topic Initialiser
# Runs as a one-shot Docker service after Kafka is healthy.
# Creates all 5 CMS topics with appropriate settings.
# =============================================================

set -e

KAFKA_BOOTSTRAP="kafka:29092"
REPLICATION_FACTOR=1        # Phase 0: 1 broker; raise to 3 in Phase 10
PARTITIONS=3                # 3 partitions allows future horizontal consumer scaling

echo "==> Waiting for Kafka to be fully ready at ${KAFKA_BOOTSTRAP}..."
sleep 5

create_topic() {
  local TOPIC=$1
  echo "  Creating topic: ${TOPIC}"
  kafka-topics \
    --bootstrap-server "${KAFKA_BOOTSTRAP}" \
    --create \
    --if-not-exists \
    --topic "${TOPIC}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION_FACTOR}"
}

# CMS Kafka topics (matches ClientOnboardedEvent, InvoiceGeneratedEvent, etc.)
create_topic "client-onboarded"
create_topic "invoice-generated"
create_topic "payment-success"
create_topic "payment-failed"
create_topic "ticket-created"
create_topic "subscription-created"

# Dead-letter topics â€” required because KAFKA_AUTO_CREATE_TOPICS_ENABLE=false.
# DefaultErrorHandler publishes to {topic}.DLT after retries exhaust;
# without these topics DeadLetterPublishingRecoverer would fail and the message
# would be lost. 1 partition is intentional: DLT consumption is single-threaded.
echo "  Creating Dead-Letter Topics (*.DLT)..."
kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP}" \
  --create --if-not-exists \
  --topic "client-onboarded.DLT" \
  --partitions 1 \
  --replication-factor "${REPLICATION_FACTOR}"

kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP}" \
  --create --if-not-exists \
  --topic "invoice-generated.DLT" \
  --partitions 1 \
  --replication-factor "${REPLICATION_FACTOR}"

kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP}" \
  --create --if-not-exists \
  --topic "payment-success.DLT" \
  --partitions 1 \
  --replication-factor "${REPLICATION_FACTOR}"

kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP}" \
  --create --if-not-exists \
  --topic "payment-failed.DLT" \
  --partitions 1 \
  --replication-factor "${REPLICATION_FACTOR}"

kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP}" \
  --create --if-not-exists \
  --topic "ticket-created.DLT" \
  --partitions 1 \
  --replication-factor "${REPLICATION_FACTOR}"

kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP}" \
  --create --if-not-exists \
  --topic "subscription-created.DLT" \
  --partitions 1 \
  --replication-factor "${REPLICATION_FACTOR}"

echo ""
echo "==> All CMS Kafka topics and DLT topics created. Listing:"
kafka-topics --bootstrap-server "${KAFKA_BOOTSTRAP}" --list

echo "==> kafka-init done."
