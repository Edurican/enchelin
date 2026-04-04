#!/usr/bin/env bash

# Hook: PreToolUse (Edit/Write)
# .env, secrets/, 민감 설정 파일 편집을 차단합니다.

set -euo pipefail

# CLAUDE_TOOL_INPUT에서 대상 파일 경로 추출
INPUT="$CLAUDE_TOOL_INPUT"

# 차단 패턴
if echo "$INPUT" | grep -qiE '(\.env|secrets/|\.secret|credentials|api[_-]?key)'; then
  echo "BLOCKED: 민감 파일 편집이 차단되었습니다. (.env, secrets/, credentials 등)" >&2
  exit 2
fi
