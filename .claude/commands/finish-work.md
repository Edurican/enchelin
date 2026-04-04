# /finish-work - 작업 완료 워크플로우

현재 브랜치의 WIP 커밋들을 정리하고 PR을 생성하는 커맨드입니다.

## 실행 절차

1. **현재 상태 확인**:
   - 현재 브랜치명 확인 (dev가 아닌지 검증)
   - 브랜치명에서 이슈 번호 추출 (예: `feat/issue-42-add-review-auth` → `#42`)
   - 커밋되지 않은 변경사항이 있으면 먼저 커밋할지 사용자에게 확인
   - `git log dev..HEAD --oneline`으로 WIP 커밋 목록 확인
   - `git diff dev..HEAD`로 전체 변경사항 분석

2. **WIP 커밋 정리 (Squash)**:
   - `git reset --soft dev`로 WIP 커밋들을 unstage
   - 전체 변경사항을 분석하여 의미 있는 커밋 메시지 작성
   - 커밋 메시지 형식: `{type}(scope): 한 줄 요약`
     - type: 브랜치명에서 추출 (feat, fix, refactor, test, chore)
     - scope: 변경 파일 경로에서 판단 (BE, FE, common)
   - 변경 범위가 크면 여러 커밋으로 나눌 수 있음 (사용자 확인)
   - 커밋 메시지를 사용자에게 보여주고 확인받은 후 커밋

3. **Push**:
   - `git push -u origin {브랜치명}`으로 원격에 push

4. **PR 생성**: `.github/PULL_REQUEST_TEMPLATE.md` 형식을 따라 PR을 생성합니다.
   - PR 제목: 커밋 메시지의 첫 줄과 동일하게
   - PR 본문 형식:
     ```
     ## 개요
     > 변경 내용 요약 (diff 분석 기반)

     ## 관련 이슈
     Closes #{이슈번호}

     ## 작업 상세 내용
     - [ ] 구현한 항목 1
     - [ ] 구현한 항목 2

     ## 체크리스트
     - [ ] 커밋 메시지 컨벤션을 지켰나요?
     - [ ] 로컬에서 테스트는 모두 통과했나요?
     - [ ] 불필요한 공백이나 주석은 제거했나요?

     ## 리뷰 요청 사항
     > 특히 봐줬으면 하는 부분
     ```
   - `gh pr create --base dev` 사용

5. **결과 보고**: 생성된 PR URL을 사용자에게 알려줍니다.

## 실행 명령 예시

```bash
# WIP squash
git reset --soft dev
git add -A
git commit -m "feat(BE): 리뷰 API에 JWT 인증 적용"

# Push 및 PR 생성
git push -u origin feat/issue-42-add-review-auth
gh pr create --base dev --title "feat(BE): 리뷰 API에 JWT 인증 적용" --body "..."
```

## 주의사항
- dev 브랜치에서는 실행하지 않습니다. 현재 브랜치가 dev면 에러를 표시합니다.
- squash 전에 반드시 현재 커밋 목록과 변경사항을 사용자에게 보여주고 확인받습니다.
- 이슈 번호를 브랜치명에서 추출할 수 없으면 사용자에게 직접 물어봅니다.

$ARGUMENTS
