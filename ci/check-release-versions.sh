#!/bin/sh
# Runs right after confirm-release, before anything is released: checks that the
# versions in the repository match the version that is about to be released.
set -eu

VERSION=$(awk '/<artifactId>jeletask<\/artifactId>/ { found = 1 }
  found && /<version>/ { gsub(/.*<version>|<\/version>.*/, ""); print; exit }' pom.xml)
VERSION="${VERSION%-SNAPSHOT}"
if [ -z "${VERSION}" ]; then
  echo "Could not find the version in pom.xml"
  exit 1
fi
echo "Releasing version: ${VERSION}"

EXPECTED="FROM ridiekel/jeletask2mqtt:${VERSION}-native"
PINNED=$(grep '^FROM ridiekel/jeletask2mqtt:' README.md || true)
if [ -z "${PINNED}" ]; then
  echo "WARNING: README.md pins no image version, nothing to check"
elif [ "${PINNED}" != "${EXPECTED}" ]; then
  echo "README.md pins the wrong image version, expected '${EXPECTED}' but found:"
  echo "${PINNED}"
  exit 1
fi

# No notes is allowed, but then publish-github-release skips the GitHub Release
if [ ! -f "release-notes/${VERSION}.md" ]; then
  echo "WARNING: no release-notes/${VERSION}.md, there will be no GitHub Release"
fi

echo "Versions are correct"
