---
description: Merge a branch into main and bring the release notes up to date. Without a branch, only update the notes, for commits already on main. Run before confirm-release.
argument-hint: "[branch]"
disable-model-invocation: true
---

# Merge to main

Argument: `$ARGUMENTS` (a branch name, or empty to only update the notes).

## 1. Prepare

Stop and report if the working tree is not clean. Fetch, switch to `main` and fast-forward it to
`origin/main`.

## 2. Merge

Skip this step without a branch.

Run `git merge --no-ff <branch>`. Never squash or rebase: GitHub only shows a pull request as merged
when its commits reach `main` unchanged.

If a conflict is anything other than the release plugin's version in `pom.xml`, stop and report it.
For that version conflict, keep the higher version.

## 3. Release notes

Find the last release with `git describe --tags --match 'jeletask-*' --abbrev=0`, and read everything
since then with `git log <tag>..HEAD`, including the diffs of what looks user-facing. This also covers
commits made directly on `main`.

The target is `release-notes/<version>.md`, where `<version>` is the version in `pom.xml` without
`-SNAPSHOT`.

- Would a user notice a change, or do they need to act on it? Then it belongs in the file. If the file
  exists, keep its text and add only what is missing.
- Changes only to dependencies, tests, CI or internals (including Renovate merges) need no notes. If
  nothing since the tag needs notes, don't create a file.
- Write in English, with only the sections that apply: `## Before you upgrade`, `## New`, `## Fixed`.
- If users have to act, or automations can break, also add it to `# Upgrading from previous version`
  in `README.md`.

The add-on `Dockerfile` under `## Home Assistant OS (local add-on)` in `README.md` pins the image. Set
its `FROM` line to `ridiekel/jeletask2mqtt:<version>-native`, also when no notes are needed.

Commit any changes with the message `docs(release): release notes voor <version>`.

## 4. Push

Push `main`. This does not start a release: that takes the manual `confirm-release` job, which you
never start yourself.

## 5. Report

Report what was merged, the notes you added, or why no notes were needed. Say so if the changes look
too big for the version, such as a new feature on a patch version: changing the version is up to the
user.

The `publish-github-release` CI job turns the notes file into the GitHub Release of the version.
Without a file there is no release. The job reads the commit on which `confirm-release` was started,
so remind the user to start it in the pipeline of the latest `main` commit.
