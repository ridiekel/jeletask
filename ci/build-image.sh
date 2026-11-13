#!/usr/bin/env bash
# Builds one image variant into the local Docker daemon and smoke tests it.
# Nothing is pushed here; ci/publish-images.sh pushes once all variants passed.
#
# Usage: ci/build-image.sh <jvm|native> <amd64|arm64>
set -euo pipefail

KIND="$1"
ARCH="$2"

. ci/image-env.sh
LOCAL_IMAGE="${IMAGE}:${VERSION}-${KIND}-${ARCH}"

if [ "${ARCH}" = "arm64" ]; then
  echo "Registering QEMU emulation for arm64..."
  docker run --privileged --rm "${BINFMT_IMAGE}" --install arm64
fi

# The legacy builder cannot add a layer to an image of another platform when the daemon keeps its images in
# containerd: the COPY of the native arm64 image fails on a missing snapshot. BuildKit can, so the native jobs, whose
# image has no buildx, borrow the plugin from the image the jvm jobs run in.
ensure_buildx() {
  if docker buildx version > /dev/null 2>&1; then
    return
  fi
  local container
  container="$(docker create "${DOCKER_CLI_IMAGE:-docker:latest}")"
  mkdir -p "${HOME}/.docker/cli-plugins"
  docker cp "${container}:/usr/local/libexec/docker/cli-plugins/docker-buildx" "${HOME}/.docker/cli-plugins/docker-buildx"
  docker rm "${container}" > /dev/null
  docker buildx version
}

echo "Building ${LOCAL_IMAGE}..."
case "${KIND}" in
  jvm)
    docker buildx build --platform "linux/${ARCH}" --load -t "${LOCAL_IMAGE}" .
    ;;
  native)
    # shellcheck disable=SC2086 # MAVEN_CLI_OPTS holds several options
    ./mvnw ${MAVEN_CLI_OPTS} -Pnative -DskipTests spring-boot:build-image \
      -Dspring-boot.build-image.imageName="${LOCAL_IMAGE}" \
      -Dspring-boot.build-image.imagePlatform="linux/${ARCH}" \
      -Dbuild-image.cache-name="${CI_PROJECT_NAME}-native-${ARCH}"
    # Buildpacks cannot set a HEALTHCHECK, so it is added as metadata on top. The licences go along: the binary has
    # the libraries compiled in, but not their notices.
    ensure_buildx
    docker buildx build --platform "linux/${ARCH}" --load -t "${LOCAL_IMAGE}" -f - target/classes/META-INF <<EOF
FROM ${LOCAL_IMAGE}
COPY LICENSE.md /licenses/
COPY third-party /licenses/third-party
HEALTHCHECK --interval=30s --timeout=10s --start-period=2m --retries=3 CMD ["/cnb/process/web","--healthcheck"]
EOF
    ;;
  *)
    echo "Unknown image kind: ${KIND}"
    exit 1
    ;;
esac

ci/smoke-test-image.sh "${LOCAL_IMAGE}" "linux/${ARCH}"
