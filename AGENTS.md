## Release notes

Check this for every change, on any branch, and commit the result together with the change, so the
notes reach `main` if the change does:

- Would a user notice the change, or do they need to act on it? Then create or update
  `release-notes/<version>.md`, where `<version>` is the version in `pom.xml` without `-SNAPSHOT`.
- Changes only to dependencies, tests, CI or internals (including Renovate merges) need no notes.
- Write in English, with only the sections that apply: `## Before you upgrade`, `## New`, `## Fixed`.
- If users have to act, or automations can break, also add it to `# Upgrading from previous version`
  in `README.md`.

The `publish-github-release` CI job turns that file into the GitHub Release of the version. Without
a file there is no release. The job reads the commit on which `confirm-release` was started, so the
notes must be on `main` before that.
