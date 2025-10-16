#!/usr/bin/env bash
set -euo pipefail

KTLINT_TASK="ktlintCheck"
DETEKT_FLAGS=""
BUILD_TASK="compileKotlin"
ESLINT_FLAGS="--max-warnings=0"
PRETTIER_FLAGS="-c"
TS_BUILD="bun x tsc -b"

if [[ "${1:-}" == "--fix" ]]; then
  KTLINT_TASK="ktlintFormat"
  DETEKT_FLAGS="--auto-correct"
  ESLINT_FLAGS="--fix --max-warnings=0"
  PRETTIER_FLAGS="-w"
fi

lint_kotlin() {
  local project_name="$1"
  echo "→ Linting and compiling $project_name..."
  ./"$project_name"/gradlew -p "$project_name" $BUILD_TASK $KTLINT_TASK detekt $DETEKT_FLAGS
}

lint_typescript() {
  local project_name="$1"
  echo "→ Linting and type-checking $project_name..."
  docker run --rm -v "$PWD/$project_name":/app -w /app oven/bun:1.1.20 bash -lc \
    "bun install && $TS_BUILD && bunx eslint $ESLINT_FLAGS . && bunx prettier $PRETTIER_FLAGS ."
}

lint_kotlin services/kotlin-backend
lint_kotlin services/kotlin-source
lint_typescript web

echo "✓ All checks passed!"
