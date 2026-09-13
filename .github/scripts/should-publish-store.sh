#!/usr/bin/env bash
# Publish to Play only when AndroidManifest version changed on master.
set -euo pipefail

VERSION_FILE="app/src/main/AndroidManifest.xml"
PATTERN='^\+\s*android:version(Name|Code)='

publish=false
reason="not master"

if [[ "${GITHUB_REF:-}" == refs/heads/master ]]; then
  if git rev-parse --verify HEAD^ >/dev/null 2>&1 && \
     git diff HEAD^ HEAD -- "$VERSION_FILE" | grep -qE "$PATTERN"; then
    publish=true
    reason="version bump in ${VERSION_FILE}"
  else
    reason="master without version bump"
  fi
fi

echo "Store publish: ${publish} (${reason})"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "publish=${publish}" >> "$GITHUB_OUTPUT"
fi
