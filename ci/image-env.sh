# Sourced by the image scripts: derives the image name and release version
# from the dotenv variables of the create-release job. A build that is no
# release, such as the native image check of a branch, sets IMAGE_VERSION.
IMAGE="${DOCKERHUB_IMAGE:-${DOCKERHUB_USERNAME}/${CI_PROJECT_NAME}}"
if [ -n "${IMAGE_VERSION:-}" ]; then
  VERSION="${IMAGE_VERSION}"
else
  TAG="${scm_tag:-${CI_COMMIT_TAG:-}}"
  if [ -z "${TAG}" ]; then
    echo "Could not find tag (scm_tag/CI_COMMIT_TAG)"
    exit 1
  fi
  VERSION="${TAG#"${CI_PROJECT_NAME}"-}"
fi
echo "Image: ${IMAGE}, version: ${VERSION}"
