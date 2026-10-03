# Sourced by the image scripts: derives the image name and release version
# from the dotenv variables of the create-release job.
IMAGE="${DOCKERHUB_IMAGE:-${DOCKERHUB_USERNAME}/${CI_PROJECT_NAME}}"
TAG="${scm_tag:-${CI_COMMIT_TAG:-}}"
if [ -z "${TAG}" ]; then
  echo "Could not find tag (scm_tag/CI_COMMIT_TAG)"
  exit 1
fi
VERSION="${TAG#"${CI_PROJECT_NAME}"-}"
echo "Image: ${IMAGE}, version: ${VERSION}"
