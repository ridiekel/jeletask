#!/bin/sh
# Publishes the test and coverage badges of the README. GitHub cannot reach the private GitLab, so the
# pipeline pushes the numbers out instead: one shields.io endpoint file per badge, on the orphan branch
# "badges" of the GitHub repository, which the README reads through img.shields.io/endpoint.
# Each change is a plain commit on top of that branch, never a force push: a concurrent run fails instead
# of silently overwriting the other one.
#
# Reads the JUnit reports of surefire (and failsafe, if any) and the JaCoCo CSV report of the test job.
# The native test reports are left out: they run the same tests again and would count them twice.
#
# Environment:
#   GITHUB_REMOTE   where to push, e.g. git@github.com:ridiekel/jeletask.git (credentials are set up by the job)
#   GITHUB_DRY_RUN  when set, everything except the actual push
#   BADGES_DIR      where to write the endpoint files (default: target/badges)
set -eu

BADGES_DIR="${BADGES_DIR:-target/badges}"
JACOCO_CSV="target/site/jacoco/jacoco.csv"

# Prints "tests failures errors skipped" summed over every report, read from the <testsuite> root element
test_totals() {
  for report in target/surefire-reports/TEST-*.xml target/failsafe-reports/TEST-*.xml; do
    [ -f "${report}" ] || continue
    grep -m 1 -o '<testsuite [^>]*>' "${report}"
  done | awk '
    function attr(name,   m) {
      if (match($0, " " name "=\"[0-9]+\"")) { m = substr($0, RSTART, RLENGTH); gsub(/[^0-9]/, "", m); return m + 0 }
      return 0
    }
    { tests += attr("tests"); failures += attr("failures"); errors += attr("errors"); skipped += attr("skipped") }
    END { print tests + 0, failures + 0, errors + 0, skipped + 0 }'
}

# Instruction coverage, rounded down like the "Total" of the JaCoCo HTML report (the number GitLab shows)
coverage_percent() {
  awk -F, 'NR > 1 { missed += $4; covered += $5 }
    END { if (missed + covered == 0) exit 1; print int(covered * 100 / (missed + covered)) }' "${JACOCO_CSV}"
}

endpoint() {
  printf '{"schemaVersion":1,"label":"%s","message":"%s","color":"%s"}\n' "$1" "$2" "$3"
}

set -- $(test_totals)
TESTS=$1 FAILED=$(($2 + $3)) SKIPPED=$4
if [ "${TESTS}" -eq 0 ]; then
  echo "No test reports found, not publishing badges"
  exit 1
fi
[ -f "${JACOCO_CSV}" ] || { echo "No ${JACOCO_CSV}, not publishing badges"; exit 1; }
COVERAGE=$(coverage_percent)

if [ "${FAILED}" -eq 0 ]; then
  TESTS_MESSAGE="$((TESTS - SKIPPED)) passed"
  TESTS_COLOR=brightgreen
else
  TESTS_MESSAGE="${FAILED} failed, $((TESTS - SKIPPED - FAILED)) passed"
  TESTS_COLOR=red
fi
[ "${SKIPPED}" -eq 0 ] || TESTS_MESSAGE="${TESTS_MESSAGE}, ${SKIPPED} skipped"

if [ "${COVERAGE}" -ge 80 ]; then COVERAGE_COLOR=brightgreen
elif [ "${COVERAGE}" -ge 60 ]; then COVERAGE_COLOR=yellow
else COVERAGE_COLOR=red
fi

: "${GITHUB_REMOTE:?GITHUB_REMOTE is not set}"
rm -rf "${BADGES_DIR}"
mkdir -p "${BADGES_DIR}"
cd "${BADGES_DIR}"
git init -q
git remote add github "${GITHUB_REMOTE}"

# Exit code 2 means the remote answered but has no such branch: the very first run starts it as an orphan.
# Anything else (no access, no network) must fail, not start a second history that a push would reject.
set +e
git ls-remote --exit-code --heads github badges > /dev/null
REMOTE_STATUS=$?
set -e
case "${REMOTE_STATUS}" in
  0) git fetch -q --depth 1 github badges && git checkout -q -b badges FETCH_HEAD ;;
  2) echo "No badges branch on ${GITHUB_REMOTE} yet, starting it"; git checkout -q --orphan badges ;;
  *) echo "Cannot read ${GITHUB_REMOTE}"; exit 1 ;;
esac

endpoint tests "${TESTS_MESSAGE}" "${TESTS_COLOR}" > tests.json
endpoint coverage "${COVERAGE}%" "${COVERAGE_COLOR}" > coverage.json
cat tests.json coverage.json

git add tests.json coverage.json
if git diff --cached --quiet; then
  echo "Badges unchanged, nothing to push"
  exit 0
fi
# [skip ci]: should this branch ever reach a CI (a mirror, GitHub Actions), it must not start a build there
git -c user.name="GitLab CI" -c user.email="ci@localhost" commit -q \
  -m "Badges for ${CI_COMMIT_SHORT_SHA:-a local build} [skip ci]"
git push ${GITHUB_DRY_RUN:+--dry-run} github HEAD:refs/heads/badges
