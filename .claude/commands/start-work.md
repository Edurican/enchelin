# /start-work - 작업 시작 워크플로우

사용자가 새로운 작업을 시작할 때 이슈를 생성하고 브랜치를 만드는 커맨드입니다.

## 실행 절차

1. **사용자 입력 수집**: 사용자에게 다음을 질문합니다.
   - 작업 유형: `feat`, `refactor`, `fix`, `test`, `chore` 중 선택
   - 작업 설명: 무엇을 구현/수정할 것인지

2. **GitHub 이슈 생성**: `gh issue create` 명령으로 이슈를 생성합니다.
   - 작업 유형에 따라 적절한 템플릿을 선택합니다:
     - `feat` → `.github/ISSUE_TEMPLATE/feature_request.md` 형식 사용, 라벨 `✨ Feature`
     - `fix` → `.github/ISSUE_TEMPLATE/bug_report.md` 형식 사용, 라벨 `🐛 Bug`
     - `refactor`, `test`, `chore` → 본문에 설명과 체크리스트만 포함
   - 이슈 제목 형식: `[FE/BE] 작업 요약` (변경 대상에 맞게 FE/BE/common 판단)
   - 사용자의 작업 설명을 바탕으로 이슈 본문을 작성합니다.

3. **브랜치 생성**: dev 브랜치에서 새 브랜치를 생성합니다.
   - 브랜치명 형식: `{type}/issue-{이슈번호}-{영문slug}`
   - 예시: `feat/issue-42-add-review-auth`, `fix/issue-15-token-expiry`
   - slug는 작업 내용을 kebab-case 영문으로 요약 (3~5 단어)

4. **체크아웃**: 생성된 브랜치로 체크아웃합니다.

5. **결과 보고**: 생성된 이슈 URL과 브랜치명을 사용자에게 알려줍니다.

## 실행 명령 예시

```bash
# 이슈 생성
gh issue create --title "[BE] 리뷰 API에 인증 적용" --label "✨ Feature" --body "..."

# 브랜치 생성 및 체크아웃
git checkout dev
git pull origin dev
git checkout -b feat/issue-42-add-review-auth
```

## 주의사항
- 반드시 dev 브랜치를 최신 상태로 pull한 후 브랜치를 생성합니다.
- 이슈 번호는 `gh issue create` 출력에서 추출합니다.
- 현재 작업 중인 변경사항이 있으면 먼저 stash하거나 커밋할지 사용자에게 확인합니다.

$ARGUMENTS
