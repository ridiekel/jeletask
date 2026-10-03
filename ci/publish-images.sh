#!/bin/sh
# Pushes the image variants built by ci/build-image.sh and combines them into
# the multi-arch tags. Runs only after every variant was built and smoke tested.
set -eu

. ci/image-env.sh
VARIANTS="jvm-amd64 jvm-arm64 native-amd64 native-arm64"

for variant in ${VARIANTS}; do
  docker push "${IMAGE}:${VERSION}-${variant}"
done

docker buildx imagetools create \
  -t "${IMAGE}:${VERSION}" \
  -t "${IMAGE}:latest" \
  "${IMAGE}:${VERSION}-jvm-amd64" \
  "${IMAGE}:${VERSION}-jvm-arm64"

# The amd64/arm64-specific native tags are kept as aliases until users have moved to latest-native
docker buildx imagetools create \
  -t "${IMAGE}:${VERSION}-native" \
  -t "${IMAGE}:latest-native" \
  -t "${IMAGE}:latest-native-amd64" \
  -t "${IMAGE}:latest-native-amd64-v1" \
  -t "${IMAGE}:latest-native-amd64-v2" \
  -t "${IMAGE}:latest-native-amd64-v3" \
  "${IMAGE}:${VERSION}-native-amd64" \
  "${IMAGE}:${VERSION}-native-arm64"
docker buildx imagetools create \
  -t "${IMAGE}:latest-native-arm64" \
  "${IMAGE}:${VERSION}-native-arm64"

docker buildx imagetools inspect "${IMAGE}:${VERSION}"
docker buildx imagetools inspect "${IMAGE}:${VERSION}-native"

for variant in ${VARIANTS}; do
  docker image rm "${IMAGE}:${VERSION}-${variant}" > /dev/null || true
done
