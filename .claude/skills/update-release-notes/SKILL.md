---
description: Check that the release notes of the current branch cover every user-facing change and every library update since the last release and still match the code, then fix what is missing or wrong. Does not merge or push.
disable-model-invocation: true
---

# Update release notes

Check the release notes of the branch you are on, and complete or correct them. This works on `main`
and on a feature branch alike. Merging, the add-on image pin and pushing are left to `merge-to-main`.

## 1. Prepare

Stop and report if the working tree is not clean: the check reads the committed history, and the fix is
committed on this branch.

Then `git fetch --tags`, and if the branch has an upstream it is behind, `git pull --ff-only`: notes written
on a stale branch miss what was merged since, and the fix commit could not be pushed. Stop and report if
the branch has diverged from its upstream.

Collect:

- the current branch (`git branch --show-current`)
- `<version>`: the version in `pom.xml` without `-SNAPSHOT`
- `<tag>`: the last release, `git describe --tags --match 'jeletask-*' --abbrev=0`
- the notes file `release-notes/<version>.md`, which may not exist yet

On a feature branch, also fetch and note which commits since `<tag>` are already on `origin/main` and
which only exist on this branch (`git merge-base HEAD origin/main`). The notes must cover both, but the
report separates them.

## 2. List the changes

Read `git log <tag>..HEAD` with the commit bodies, and the diffs of everything that might be
user-facing. Don't rely on the commit subject alone: a `refactor` or `test` commit can change
behaviour, and a `feat` can be reverted or reworked later on.

For each change, decide whether a user would notice it or has to act on it: MQTT topics and payloads,
Home Assistant discovery, `config.json` keys, validation and defaults, environment variables, the
Docker image and health check, behaviour towards the central unit, log messages users rely on.

Changes only to tests, CI or internals need no notes. Library updates go in their own section, see the
next step.

## 3. List the library updates

Don't go by the Renovate commits: compare the files at `<tag>` with `HEAD`, so a library updated twice
shows once with its version at the tag and at `HEAD`, and one that was updated and reverted doesn't
show at all. Look at:

- `pom.xml`: the Spring Boot parent, `<properties>`, dependencies (with their `<scope>`) and plugins,
  also those in profiles
- `Dockerfile`: the base images, also when only the pinned digest changed
- `.mvn/wrapper/maven-wrapper.properties`, the CI files and the Dockerfiles under `src/test`

Sort each update into one of two groups:

- **Production**: what ends up in the published images or builds them: the Spring Boot parent, the
  dependencies in `compile` or `runtime` scope, the Java version, the base images in `Dockerfile`, and
  the plugins that build the jar or the native image (`spring-boot-maven-plugin`,
  `native-maven-plugin`).
- **Supporting**: everything else: dependencies in `test` or `provided` scope (such as Lombok), the
  other Maven plugins (Sonar, JaCoCo, PIT, compiler, ...), the Maven wrapper, CI images and the test
  containers.

For the Spring Boot parent, give its own version only; don't list the libraries it manages.

## 4. Compare with the notes

Check in both directions:

- **Missing**: a user-facing change or a library update that the notes don't mention.
- **Wrong or outdated**: an entry that no longer matches the code, because a later commit changed,
  renamed or reverted it. Verify every concrete name and value in the notes (environment variables,
  `config.json` keys, MQTT values, defaults, examples of log messages, library versions) against the
  code at `HEAD`. Also check general claims such as "no configuration changes are needed".
- **Wrong section**: anything a user has to act on, or that can break automations, belongs under
  `## Before you upgrade`, not only under `## New` or `## Fixed`. A library update that users notice
  (other behaviour, a new Java version they run themselves) goes there too, besides its line under
  `## Dependencies`.
- **README**: the same actions and breaking changes must be in `# Upgrading from previous version` in
  `README.md`, and that section must agree with the notes.

## 5. Fix

- If nothing since `<tag>` needs notes and no library changed, and the file does not exist, don't
  create it. Library updates alone are enough to create it.
- Keep the existing text and its style. Add what is missing and correct what is wrong; don't rewrite
  entries that are fine.
- Write in English, with only the sections that apply, in this order: `## Before you upgrade`,
  `## New`, `## Fixed`, `## Dependencies`.
- Remove an entry only when its change is gone from the code. If an entry looks unnecessary but still
  holds, leave it and mention it in the report.

`## Dependencies` has a subsection per group, only when that group has updates, one line per library
in alphabetical order:

```markdown
## Dependencies

### Production

- Spring Boot 4.1.1 → 4.1.2
- `org.eclipse.paho.client.mqttv3` 1.2.5 → 1.2.6
- Base image `gcr.io/distroless/base-debian13:nonroot`: new digest

### Supporting (tests and build)

- `pitest-maven` 1.30.1 → 1.30.2
```

Commit the changes on the current branch with the message `docs(release): release notes voor <version>`
and a body explaining what was missing or wrong. Don't push.

## 6. Report

Give a short table of the changes since `<tag>`: covered already, added, corrected, or no notes needed
(with the reason). On a feature branch, mark which changes come from this branch. List the library
updates per group.

Also report:

- entries you doubted but left as they are
- library updates you weren't sure which group they belong to
- when the changes look too big for `<version>`, such as a new feature or a breaking change on a patch
  version: changing the version is up to the user
