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
    ;;
  *)
    echo "Unknown image kind: ${KIND}"
    exit 1
    ;;
esac

ci/smoke-test-image.sh "${LOCAL_IMAGE}" "linux/${ARCH}"
