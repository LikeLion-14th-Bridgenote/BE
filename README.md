# Bridgenote Backend

회의 중 문화적 오해를 실시간으로 줄이고, 회의 후에는 맥락까지 담은 회의록을 제공하는 **AI 협업 서비스의 메인 백엔드 (Spring Boot)**.

인증·프로필·회의·실시간(WebSocket)을 담당한다. AI 처리(번역·문화 분석·회의록 생성)는 별도 FastAPI 서버(`bridgenote-AI`)가 담당하며, 이 서버가 내부 HTTP로 호출한다.

## Tech Stack

| 항목 | 값 |
| --- | --- |
| Language | Java 21 (Temurin 권장) |
| Framework | Spring Boot 4.0.7 |
| Build | Gradle (Wrapper) |
| DB | Supabase (PostgreSQL + pgvector) |
| Auth | Supabase Auth 연동 |
| Realtime | WebSocket (STOMP) |
| STT | Deepgram (Agora 미사용 확정) |
| Docs | Springdoc OpenAPI (Swagger UI) |

## Prerequisites

- JDK 21 (Temurin 권장)
- Supabase 프로젝트 (PostgreSQL 15.x + pgvector 확장)
- Git

## Project Structure

```
com.bridgenote
 ├─ auth         # 회원가입/로그인/로그아웃/탈퇴, 토큰 (Supabase Auth 연동)
 ├─ user         # 마이페이지, 프로필(언어·문화·직무·조직) 조회/수정
 ├─ meeting      # 회의 생성/조회/참가/동의/종료, 초대링크
 ├─ participant  # 참가자, 화자 번호(speaker_index), 데이터 처리 동의
 ├─ realtime     # WebSocket 채널, 오디오 업스트림, 발화자 전환, AI 서버 호출
 └─ common       # 예외/응답/JWT/설정/유틸
```

각 도메인은 `controller / service / repository / domain / dto` 구조를 따른다.

## Ownership (Module → Owner)

| 모듈 | 담당 |
| --- | --- |
| auth | 조수민 |
| user | 조수민 |
| meeting | 전진수 |
| participant | 전진수 |
| realtime | 전진수 |
| common | 공통 |

## Environment

- 기본 포트: `8080`
- 프로필: `dev` / `prod` (공통 `application.yml`에서 `active=dev`)
- 환경변수는 `.env.example` 참고 (실제 값 커밋 금지)
- 로컬 개발 값은 gitignore 대상인 `application-dev.yml`에 채우거나, 환경변수로 주입한다.

### AI 서버 연동

`realtime` 모듈이 확정 발화(`is_final`)를 받으면 AI 서버(`bridgenote-AI`)의 내부 API를 HTTP로 호출한다.

```
AI_SERVER_URL=http://localhost:8000
```

- `POST {AI_SERVER_URL}/ai/analyze` — 문화 오해 판정(게이트) + 각주 + 리라이트
- `POST {AI_SERVER_URL}/ai/translate` — 다국어 번역
- `POST {AI_SERVER_URL}/ai/minutes` — 회의록 생성(배치)

## Configuration

**개발:**

```
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=never
```

**운영:**

```
spring.jpa.hibernate.ddl-auto=validate
# (권장) Flyway로 마이그레이션 관리
```

## How to Run (Local)

**IntelliJ**

1. 프로젝트 열기 → Gradle 자동 동기화
2. Gradle 설정
   - Build and run using: **Gradle**
   - Run tests using: **Gradle**
   - Distribution: **Wrapper**
   - Gradle JVM: **JDK 21**
3. `.env` 세팅 (`.env.example` 복사 후 값 채우기) — 또는 `application-dev.yml`에 직접 입력
4. `BridgenoteApplication` 실행

**커맨드라인**

```
./gradlew bootRun
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## API 도메인 (요약)

| 도메인 | 경로 | 담당 |
| --- | --- | --- |
| 인증 | `/auth/...` | 조수민 |
| 프로필 | `/users/...` | 조수민 |
| 회의 | `/api/meetings/...` | 전진수 |
| 실시간 | `/ws/meetings/{id}` | 전진수 |

> 상세 명세는 노션 API 명세서 참고.

## Git Workflow

- 기본 브랜치: `main`
- 작업 브랜치 규칙: `feature/<기능>-<이름>`
  - 예) `feature/meeting-jinsoo`, `feature/auth-cho`

```
git checkout -b feature/meeting-jinsoo
git add .
git commit -m "[ADD] 회의 생성 API 추가"
./gradlew clean build
git push -u origin feature/meeting-jinsoo
```

### PR 규칙

- `main`에 직접 push 금지 → 반드시 브랜치를 파서 PR로만 병합 (초기 세팅 커밋만 예외)
- 민감 파일(`application-*.yml`, `.env`, 키 파일)은 반드시 `.gitignore`에 추가 (이미 포함)
- PR 제목 예시:
  - `feat(meeting): implement meeting creation API`
  - `fix(realtime): null check on speaker switch`
- PR 본문에 변경 요약 / 테스트 방법 / 관련 이슈 포함
- CI에서 `./gradlew clean build` 통과 필수

### 원격 main과 충돌 시

```
git pull origin main
# 충돌 해결 후
git add .
git commit -m "merge: integrate remote main into local main"
git push origin main
```

## Commit Convention

| 태그 | 의미 |
| --- | --- |
| `[INIT]` | 초기 세팅 |
| `[ADD]` | 기능 추가 |
| `[FIX]` | 버그 수정 |
| `[REFACTOR]` | 리팩토링 |
| `[HOTFIX]` | 배포 중 긴급 수정 |

예) `[ADD] 회의 참가 API 추가`, `[FIX] WebSocket 재연결 오류 수정`

## DTO Naming

`엔티티명 + 행위(CRUD/Get) + 형태(Req/Res) + Dto`

- 회의 생성: `MeetingCreateReqDto`, `MeetingCreateResDto`
- 회의 조회: `MeetingGetReqDto`, `MeetingGetResDto`
- 비CRUD(참가): `MeetingJoinReqDto`, `MeetingJoinResDto`

## 테스트 계정 (제출용)

- `ko_host@test.com` / `[비밀번호]` (한국어, PM/호스트)
- `vn_dev@test.com` / `[비밀번호]` (베트남어, 개발자)

> 로그인만 해도 완성된 샘플 회의록이 보이도록 `ko_host` 계정에 미리 데이터를 남겨둔다.

## 관련 문서

- 기능 명세서 (노션)
- ERD (ERDCloud)
- API 명세서 (노션)
- AI 서버: `bridgenote-AI` 레포
