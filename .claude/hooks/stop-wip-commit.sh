#!/usr/bin/env bash

# Hook: Stop
# 세션 종료 시 커밋되지 않은 변경사항을 WIP 커밋으로 자동 저장합니다.

set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || REPO_ROOT="$CLAUDE_PROJECT_DIR"
cd "$REPO_ROOT" || exit 0

# 모든 변경사항 스테이징
git add -A 2>/dev/null || true

# 민감 파일 스테이징에서 제외
git reset HEAD -- '*.env*' 'secrets/' '*.secret' '*credentials*' 2>/dev/null || true

# 커밋할 게 없으면 종료
if git diff-index --quiet HEAD 2>/dev/null; then
  exit 0
fi

# 변경 파일 수와 목록 수집
FILE_COUNT=$(git diff --cached --name-only | wc -l | tr -d ' ')
CHANGED_FILES=$(git diff --cached --name-only | head -5)

# scope 자동 추출: 변경 파일 경로 기반
HAS_BE=$(git diff --cached --name-only | grep -c '^backend/' || true)
HAS_FE=$(git diff --cached --name-only | grep -c '^frontend/' || true)

if [ "$HAS_BE" -gt 0 ] && [ "$HAS_FE" -gt 0 ]; then
  SCOPE="common"
elif [ "$HAS_BE" -gt 0 ]; then
  SCOPE="BE"
elif [ "$HAS_FE" -gt 0 ]; then
  SCOPE="FE"
else
  SCOPE="common"
fi

COMMIT_MSG="WIP(${SCOPE}): ${FILE_COUNT}개 파일 업데이트

변경 파일:
$CHANGED_FILES"

# 커밋 실행
echo "$COMMIT_MSG" | git commit -F - --no-verify 2>/dev/null || true
