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

Read `.claude/skills/update-release-notes/SKILL.md` and follow its steps 2 to 5 on `main`, after the
merge: they decide what goes into `release-notes/<version>.md` and `README.md`, including the
`## Dependencies` section with the library updates, and commit it. They cover everything since the last
release, also commits made directly on `main`. Keep the list of changes and library updates for the
report.

The add-on `Dockerfile` under `## Home Assistant OS (local add-on)` in `README.md` pins the image. Set
its `FROM` line to `ridiekel/jeletask2mqtt:<version>-native`, also when no notes are needed, and commit
it with the message `docs(release): release notes voor <version>`, or in the same commit as the notes.

## 4. Push

Push `main`. This does not start a release: that takes the manual `confirm-release` job, which you
never start yourself.

## 5. Report

Report what was merged, the notes you added, or why no notes were needed, in the form of step 6 of
`update-release-notes`. Say so if the changes look too big for the version, such as a new feature on a
patch version: changing the version is up to the user.

The `publish-github-release` CI job turns the notes file into the GitHub Release of the version.
Without a file there is no release. The job reads the commit on which `confirm-release` was started,
so remind the user to start it in the pipeline of the latest `main` commit.
