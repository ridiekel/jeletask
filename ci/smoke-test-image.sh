#!/usr/bin/env bash
# Starts a built image next to an MQTT broker and a stand-in central unit, and
# waits until the application reports that it has started.
#
# Usage: ci/smoke-test-image.sh <image> <platform> [timeout-seconds]
set -euo pipefail

IMAGE="$1"
PLATFORM="$2"
TIMEOUT="${3:-600}"

MOSQUITTO_IMAGE="${SMOKE_MOSQUITTO_IMAGE:-eclipse-mosquitto:2.1.2-alpine}"
SOCAT_IMAGE="${SMOKE_SOCAT_IMAGE:-alpine/socat:1.8.1.3}"
NAME="jeletask-smoke-${CI_JOB_ID:-$$}"

cleanup() {
  docker rm -f "${NAME}-app" "${NAME}-mqtt" "${NAME}-central" > /dev/null 2>&1 || true
  docker network rm "${NAME}" > /dev/null 2>&1 || true
}
trap cleanup EXIT

show_logs() {
  echo "----- application log -----"
  docker logs "${NAME}-app" 2>&1 | tail -n 200
}

echo "Smoke testing ${IMAGE} (${PLATFORM})..."
docker network create "${NAME}" > /dev/null

docker run -d --name "${NAME}-mqtt" --network "${NAME}" --network-alias mqtt \
  "${MOSQUITTO_IMAGE}" mosquitto -c /mosquitto-no-auth.conf > /dev/null

# The application only needs the central unit to accept TCP connections to start
docker run -d --name "${NAME}-central" --network "${NAME}" --network-alias central \
  "${SOCAT_IMAGE}" -u TCP-LISTEN:1234,fork,reuseaddr /dev/null > /dev/null

docker create --name "${NAME}-app" --platform "${PLATFORM}" --network "${NAME}" \
  -e TELETASK_CONFIGFILE=file:/tmp/config.json \
  -e TELETASK_CENTRAL_HOST=central \
  -e TELETASK_CENTRAL_PORT=1234 \
  -e TELETASK_MQTT_HOST=mqtt \
  "${IMAGE}" > /dev/null
docker cp src/test/resources/test-config.json "${NAME}-app:/tmp/config.json"
docker start "${NAME}-app" > /dev/null

for ((waited = 0; waited < TIMEOUT; waited += 5)); do
  if docker logs "${NAME}-app" 2>&1 | grep -q "Teletask2Mqtt .* started!"; then
    echo "Smoke test passed after ~${waited}s"
    exit 0
  fi
  if [ "$(docker inspect -f '{{.State.Running}}' "${NAME}-app")" != "true" ]; then
    echo "Smoke test failed: container stopped"
    show_logs
    exit 1
  fi
  sleep 5
done

echo "Smoke test failed: not started within ${TIMEOUT}s"
show_logs
exit 1
