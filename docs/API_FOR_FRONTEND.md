# Bridgenote BE — 프론트 연동 API 요약

- Base URL: `http://<host>:8080`
- 인증: 로그인 후 받은 Supabase `access_token`을 `Authorization: Bearer <token>` 헤더로. (WS는 `?token=`)
- 응답 JSON은 **snake_case**. 에러는 `{ "message": "..." }`.

## 인증 (담당: 조수민)
| method | endpoint | 설명 |
| --- | --- | --- |
| POST | `/auth/signup` | 회원가입 (email, password, name, language, culture, job, organization) |
| POST | `/auth/login` | 로그인 → `access_token`, `refresh_token` 반환 |
| POST | `/auth/logout` | 로그아웃 |

## 프로필 (담당: 조수민)
| method | endpoint | 설명 |
| --- | --- | --- |
| GET | `/users/me` | 내 프로필 (name·language·culture·job·organization) |
| PATCH | `/users/me` | 프로필 수정 |
| DELETE | `/users/me` | 회원 탈퇴 |

## 회의 (담당: 전진수)
| method | endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/meetings` | 회의 생성 (title, description?, expected_count?) → id·invite_code·invite_url·status |
| GET | `/api/meetings` | 내 회의 목록 |
| GET | `/api/meetings/{id}` | 회의 정보 + 참가자 목록 |
| POST | `/api/meetings/{id}/consent` | 데이터 처리 동의 (join 전 필수) |
| POST | `/api/meetings/{id}/join` | 회의 참가 (invite_code) → speaker_index·profile |
| POST | `/api/meetings/{id}/end` | 회의 종료 (주최자만) → 회의록 생성 트리거 |

## 회의록 아카이브 (담당: 전진수)
| method | endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/meetings/{id}/minutes` | 회의록. `status`: pending/ready/failed. ready면 `minutes[{language, job_role, decisions[], discussions[], action_items[{task, owner, deadline}]}]` |
| GET | `/api/meetings/{id}/utterances?page=0&size=20` | **전체 전사 기록**. `utterances[{sentence_id, speaker_index, speaker, spoken_at, source_lang, source_text, translations[{lang,text}]}]` + 페이지네이션 |
| GET | `/api/meetings/{id}/cultural-notes` | **문화 가이드**. `total`, `by_type[{note_type,count}]`, `notes[{sentence_id, speaker, risk_level, note_type, speaker_intent, listener_misread, advice, rewrite_text, created_at}]` |

> `minutes.language` = 언어 스위처, `minutes.job_role` = 관점별 요약(dev/design/pm/sales/research/etc). `note_type` = 문화 이해/커뮤니케이션/업무 스타일.

## 실시간 WebSocket (담당: 전진수)
연결: `ws://<host>:8080/ws/meetings/{id}?token=<access_token>`
종료코드: 4401(인증실패) · 4403(consent/join 필요) · 4404(회의없음) · 4409(종료된 회의) · 1000(정상)

> 호스트를 포함한 모든 사용자는 `consent → join → WebSocket 연결` 순서로 입장한다. 회의 생성만으로 호스트가 참가자로 자동 등록되지는 않는다.

### 클라 → 서버
```jsonc
// 발화자 전환
{ "type": "speaker_switch", "speaker_index": 0 }
// 오디오 청크(STT). base64 오디오. speaker_index 생략 시 현재 화자로 태깅
{ "type": "audio_chunk", "speaker_index": 0, "seq": 12, "data": "<base64>" }
```

### 서버 → 클라
```jsonc
// 자막(원문). is_final=false는 "인식 중"
{ "type": "caption", "sentence_id": "...", "speaker_index": 0, "source_lang": "ko", "source_text": "...", "is_final": true }
// 번역(청자 언어별). 클라는 자기 언어만 표시
{ "type": "translation", "sentence_id": "...", "target_lang": "en", "text": "..." }
// 문화 경고 패널
{ "type": "warning", "sentence_id": "...", "risk_level": "Med", "note_type": "문화 이해",
  "speaker_intent": "...", "listener_misread": "...", "advice": "...", "rewrite_text": "..." }
// 회의 상태 / 참가자
{ "type": "meeting_started", "meeting_id": "...", "started_at": "..." }
{ "type": "meeting_ended",   "meeting_id": "...", "ended_at": "..." }
{ "type": "participant_joined", "profile_id": "...", "speaker_index": 0, "nickname": "...", "language": "ko" }
{ "type": "participant_left",   "profile_id": "..." }
```

### 화면 매핑 힌트
- 자막 좌/우 정렬: caption `speaker_index` → 참가자 매핑, 본인이면 우측
- ⚠️ 아이콘: `warning`이 온 `sentence_id`의 caption에 부착 + 우측 "문화 경고" 패널 카드
- 회의 종료 시 `meeting_ended` 수신 → 회의록 대기 화면, 이후 `GET /minutes` 폴링(pending→ready)
