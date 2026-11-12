#!/usr/bin/env bash
# Starts a built image next to an MQTT broker and a stand-in central unit, and
# waits until the image's HEALTHCHECK reports the container as healthy. Healthy
# only says the bridge connected, so the test then checks both directions over
# MQTT: the bridge publishes that it is online, and it stops when asked to on
# its stop topic.
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
  echo "----- health checks -----"
  docker inspect -f '{{range .State.Health.Log}}{{.Start}} exit={{.ExitCode}} {{.Output}}{{end}}' "${NAME}-app"
  echo "----- application log -----"
  docker logs "${NAME}-app" 2>&1 | tail -n 200
  echo "----- stand-in central unit ($(docker inspect -f '{{.State.Status}}' "${NAME}-central")) -----"
  docker logs "${NAME}-central" 2>&1 | tail -n 50
}

echo "Smoke testing ${IMAGE} (${PLATFORM})..."
docker network create "${NAME}" > /dev/null

docker run -d --name "${NAME}-mqtt" --network "${NAME}" --network-alias mqtt \
  "${MOSQUITTO_IMAGE}" mosquitto -c /mosquitto-no-auth.conf > /dev/null

# The stand-in central unit sends an acknowledge byte (0x0A) every 0.2s, which
# acknowledges every message, so the application considers itself connected.
# The command must not contain ':' or ',', socat splits the address on those.
docker run -d --name "${NAME}-central" --network "${NAME}" --network-alias central \
  "${SOCAT_IMAGE}" TCP-LISTEN:1234,fork,reuseaddr SYSTEM:'while true; do echo; sleep 0.2; done' > /dev/null

docker create --name "${NAME}-app" --platform "${PLATFORM}" --network "${NAME}" \
  -e TELETASK_CONFIGFILE=file:/tmp/config.json \
  -e TELETASK_CENTRAL_HOST=central \
  -e TELETASK_CENTRAL_PORT=1234 \
  -e TELETASK_CENTRAL_ID=smoke \
  -e TELETASK_MQTT_HOST=mqtt \
  "${IMAGE}" > /dev/null
docker cp src/test/resources/test-config.json "${NAME}-app:/tmp/config.json"
docker start "${NAME}-app" > /dev/null

fail() {
  echo "Smoke test failed: $1"
  show_logs
  exit 1
}

running() {
  [ "$(docker inspect -f '{{.State.Running}}' "${NAME}-app")" = "true" ]
}

# Waits until the given check passes, for as long as the container runs and
# the timeout allows. Usage: wait_for <what> <check...>
wait_for() {
  local what="$1"
  shift
  for (( ; waited < TIMEOUT; waited += 5)); do
    if "$@"; then
      echo "${what} after ~${waited}s"
      return 0
    fi
    running || fail "container stopped while waiting: ${what}"
    sleep 5
  done
  fail "not within ${TIMEOUT}s: ${what}"
}

is_healthy() {
  local health
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${NAME}-app")"
  case "${health}" in
    unhealthy | none) fail "health status is ${health}" ;;
  esac
  [ "${health}" = "healthy" ]
}

# The retained availability: published by the bridge once the central unit answers
is_online() {
  [ "$(docker exec "${NAME}-mqtt" mosquitto_sub -t "${BASE_TOPIC}/state" -C 1 -W 3 2> /dev/null)" = "online" ]
}

# Not retained and the bridge subscribes late in its startup, so asked again until it listens
stopped_on_request() {
  docker exec "${NAME}-mqtt" mosquitto_pub -t "${BASE_TOPIC}/stop" -m "smoke test" || true
  sleep 2
  ! running
}

BASE_TOPIC="teletask2mqtt/smoke"
waited=0
wait_for "Healthy" is_healthy
wait_for "Online on MQTT" is_online
# From here on a stopped container is the goal, not a failure
for (( ; waited < TIMEOUT; waited += 5)); do
  if stopped_on_request; then
    exit_code="$(docker inspect -f '{{.State.ExitCode}}' "${NAME}-app")"
    [ "${exit_code}" = "0" ] || fail "stopped with exit code ${exit_code} after a message on its stop topic"
    echo "Smoke test passed: stopped on request after ~${waited}s"
    exit 0
  fi
  sleep 3
done
fail "did not stop within ${TIMEOUT}s after messages on its stop topic"
