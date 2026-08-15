# Bridgenote 배포 가이드 (Gabia 단일 서버)

한 서버에 **BE(Spring, 공인 8080)** + **AI(FastAPI, 내부 8000)** 를 함께 띄운다.
DB/Auth는 외부 **Supabase**. BE가 AI를 `http://localhost:8000`으로 내부 호출한다.

```
[클라이언트] ──HTTPS/WS──> [공인IP:8080 BE] ──localhost:8000──> [AI]
                                  └──────────> [Supabase DB/Auth]
```

## 0. 사전 준비
- JDK 21, Python 3.11+, git
- 방화벽: **8080만 공개**, 8000은 내부(localhost)만
- Supabase: pgvector 확장 ON (Dashboard → Database → Extensions → `vector`)

## 1. AI 서버 (FastAPI)
```bash
cd bridgenoteAI
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env      # 값 채우기: LLM_API_KEY, SUPABASE_DB_URL(pooler), STT_API_KEY 등
python scripts/ingest_corpus.py   # culture_corpus 코퍼스 적재(최초 1회)
uvicorn app.main:app --host 127.0.0.1 --port 8000   # 내부 바인딩만
```
> `SUPABASE_DB_URL`은 **Session pooler URL** 사용(direct host `db.<ref>.supabase.co`는 IPv6/DNS 이슈).

### systemd (예시) `/etc/systemd/system/bridgenote-ai.service`
```ini
[Unit]
Description=Bridgenote AI
After=network.target
[Service]
WorkingDirectory=/opt/bridgenoteAI
ExecStart=/opt/bridgenoteAI/venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000
Restart=always
[Install]
WantedBy=multi-user.target
```

## 2. BE 서버 (Spring)
```bash
cd bridgenoteBE
JAVA_HOME=/path/to/jdk-21 ./gradlew clean bootJar
# 설정: 환경변수 또는 application-dev.yml (application-dev.yml.example 참고)
java -jar build/libs/bridgenote-0.0.1-SNAPSHOT.jar
```
필수 환경변수(또는 application-dev.yml):
```
SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD   # datasource
SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY, SUPABASE_SECRET_KEY  # auth (없으면 로그인 500)
DEEPGRAM_API_KEY                                          # STT
AI_SERVER_URL=http://localhost:8000                       # BE→AI (기본값 동일)
INVITE_BASE_URL=https://<공개도메인>                        # 초대 링크 base
```
> `ddl-auto: update`라 최초 기동 시 테이블 자동 생성(meeting/participant/utterance/utterance_translation/cultural_note/meeting_minutes/users).

### systemd (예시) `/etc/systemd/system/bridgenote-be.service`
```ini
[Unit]
Description=Bridgenote BE
After=network.target bridgenote-ai.service
[Service]
WorkingDirectory=/opt/bridgenoteBE
Environment=JAVA_HOME=/opt/jdk-21
EnvironmentFile=/opt/bridgenoteBE/.env.prod
ExecStart=/opt/jdk-21/bin/java -jar /opt/bridgenoteBE/app.jar
Restart=always
[Install]
WantedBy=multi-user.target
```

## 3. 기동 & 확인
```bash
sudo systemctl enable --now bridgenote-ai bridgenote-be
curl http://localhost:8000/health        # AI
curl http://<공인IP>:8080/ping            # BE
```

## 4. 배포 전 체크리스트
- [ ] Supabase pgvector ON + culture_corpus 적재됨
- [ ] AI `.env` 키 채움 (LLM/STT/DB)
- [ ] BE Supabase **auth** 3키 채움 (url/publishable/secret) — 로그인 필수
- [ ] 8080 공개, 8000 내부
- [ ] 로그인→회의생성→발화→각주/전사/회의록 조회 풀 플로우 1회 통과
