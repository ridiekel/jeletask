#!/usr/bin/env bash
# Starts a built image next to an MQTT broker and a stand-in central unit, and
# waits until the image's HEALTHCHECK reports the container as healthy.
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
  -e TELETASK_MQTT_HOST=mqtt \
  "${IMAGE}" > /dev/null
docker cp src/test/resources/test-config.json "${NAME}-app:/tmp/config.json"
docker start "${NAME}-app" > /dev/null

for ((waited = 0; waited < TIMEOUT; waited += 5)); do
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${NAME}-app")"
  case "${health}" in
    healthy)
      echo "Smoke test passed: healthy after ~${waited}s"
      exit 0
      ;;
    unhealthy | none)
      echo "Smoke test failed: health status is ${health}"
      show_logs
      exit 1
      ;;
  esac
  if [ "$(docker inspect -f '{{.State.Running}}' "${NAME}-app")" != "true" ]; then
    echo "Smoke test failed: container stopped"
    show_logs
    exit 1
  fi
  sleep 5
done

echo "Smoke test failed: not healthy within ${TIMEOUT}s"
show_logs
exit 1
