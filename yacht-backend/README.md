# Yacht Backend

실시간 멀티플레이 Yacht Dice 게임을 위한 Spring Boot 백엔드입니다.  
개인 프로젝트로, 인증/게임룸/실시간 게임 진행 로직을 한 서비스 안에서 구현했습니다.

## 프로젝트 개요

- 목적: JWT 인증 + WebSocket(STOMP) 기반 실시간 턴제 게임 서버 구현
- 핵심 포인트 1: REST API로 회원/인증/게임룸 조회 제공
- 핵심 포인트 2: WebSocket으로 방 입장/준비/게임 시작/게임 액션 동기화
- 핵심 포인트 3: 턴 타임아웃 시 자동 점수 처리 및 다음 턴 전환

## 기술 스택

- Java 21
- Spring Boot 4.0.1
- Spring Web, Spring Security, Spring Data JPA, WebSocket(STOMP + SockJS)
- MySQL 8
- Gradle

## 실행 방법

### 1) 환경 변수 설정

`.env.example`을 참고해서 로컬 전용 `.env`를 만듭니다. `.env`는 Git에 커밋하지 않습니다.

```properties
MYSQL_ROOT_PASSWORD=...
MYSQL_DATABASE=database
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
```

### 2) DB/Redis 실행 (Docker)

```bash
docker compose up -d
```

`docker-compose.yml`은 개발용 MySQL/Redis만 실행합니다.

### 3) 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows:

```bash
gradlew.bat bootRun
```

기본 프로필은 `local-all`, 컨텍스트 패스는 `/api`입니다.

### 4) API 서버 + 게임 서버 로컬 통합 실행

```bash
./gradlew bootJar
docker compose -f docker-compose.local.yml up --build
```

Windows:

```bash
gradlew.bat bootJar
docker compose -f docker-compose.local.yml up --build
```

로컬 통합 실행 포트:

- API 서버: `http://localhost:8080/api`
- 게임 서버 1: `ws://localhost:8081/api/ws-stomp`
- 게임 서버 2: `ws://localhost:8082/api/ws-stomp`
- MySQL: `localhost:3311`
- Redis: `localhost:6379`

## 설정 값

`src/main/resources/application.yml`

- `spring.profiles.active=local-all`
- `server.servlet.context-path=/api`
- `jwt.expiration=900` (액세스 토큰 만료, 초)
- `jwt.refresh_expiration=86400` (리프레시 토큰 만료, 초)
- `game.rule.turn_limit_minutes=3`
- `game.rule.user_limit_per_room=4`

`src/main/resources/application-dev.yml`

- MySQL 접속 정보
- `jwt.secret`은 `JWT_SECRET` 환경 변수에서 주입

프로필 파일:

- `application-dev.yml`: 로컬 DB/JWT 설정
- `application-api.yml`: API 서버 역할
- `application-game.yml`: 게임 서버 역할
- `application-all.yml`: 단일 서버 로컬 실행 역할
- `application-api-swagger.yml`: API 개발 Swagger 활성화

Swagger는 `local-api` 프로필에서만 활성화됩니다.

```bash
./gradlew bootRun --args="--spring.profiles.active=local-api"
```

```text
http://localhost:8080/api/swagger-ui
http://localhost:8080/api/v3/api-docs
```

## API 요약

Base URL: `http://localhost:8080/api`

### Auth

- `POST /auth/login` - 로그인
- `POST /auth/refresh` - 액세스 토큰 재발급 (쿠키의 `refresh_token` 사용)
- `POST /auth/logout` - 로그아웃

로그인/재발급 응답 본문은 `accessToken`(data)이고, refresh token은 HttpOnly 쿠키로 내려갑니다.

### User

- `POST /users` - 회원가입
- `GET /users/{nickname}` - 유저 조회
- `DELETE /users/me` - 내 계정 삭제 (인증 필요)

### Room

- `POST /rooms` - 게임룸 생성 (인증 필요)
- `GET /rooms` - 게임룸 목록(Slice 페이징)
- `GET /rooms/{roomId}` - 게임룸 상세

## WebSocket(STOMP) 요약

- 연결 엔드포인트: 방 생성/조회 응답의 `gameServer.wsUrl` (SockJS)
- 헤더: `Authorization: Bearer {accessToken}`
- 발행 prefix: `/pub`
- 구독 prefix: `/sub`, 개인 에러 큐 `/user/queue/errors`

### 발행(Publish)

- `/pub/rooms/{roomId}/enter` - 방 입장
- `/pub/rooms/{roomId}/leave` - 방 퇴장
- `/pub/rooms/{roomId}/toggleReady` - 준비 상태 토글
- `/pub/rooms/{roomId}/start` - 게임 시작 (방장)
- `/pub/games/{roomId}/action` - 게임 액션 전송

`/pub/games/{roomId}/action` payload 예시:

```json
{
  "type": "ROLL",
  "roomId": 1,
  "keepIndices": [],
  "scoreCategory": null
}
```

### 구독(Subscribe)

- `/sub/rooms/{roomId}` - 방/게임 이벤트 수신
- `/user/queue/errors` - 개인 에러 수신

메시지 타입 예시: `ENTER`, `QUIT`, `START`, `TOGGLE_READY`, `ROLL`, `KEEP_TOGGLE`, `SELECT_SCORE`, `TIME_OUT`, `GAME_OVER`, `ERROR`

## 테스트

```bash
./gradlew test
```

## 구현 방향 및 개선 아이디어

- 게임 룸/회원/기록은 DB(MySQL)에 저장
- 실시간 참여자 상태 및 현재 게임 상태는 `ConcurrentHashMap` 기반 인메모리 저장소 사용
- 단일 서버 기준으로 실시간 동기화에 집중한 구조

향후 개선 아이디어:

- Redis 기반 상태 저장/분산 락으로 멀티 인스턴스 확장
- OpenAPI(Swagger) 문서 자동화
- WebSocket 통합 테스트 및 부하 테스트 강화
