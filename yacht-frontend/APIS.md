# 🎲 Yacht Game API & WebSocket 명세서

## 1. 기본 설정 (Base Config)

* **인증 방식**: JWT (Access Token) + Cookie (Refresh Token)
* **API 응답 공통 포맷**:
성공 시 항상 `data` 필드로 감싸서 응답합니다.
```json
{ "data": "실제 결괏값" }

```


* **에러 공통 포맷**: (HTTP 상태 코드 4xx, 5xx)
```json
{ "code": "U001", "message": "사용 중인 아이디입니다." }

```


* **Authorization 헤더**: 로그인이 필요한 모든 HTTP 요청과 WebSocket 연결 시 `Authorization: Bearer <AccessToken>` 헤더를 포함해야 합니다.

---

## 2. HTTP REST API 명세

### 🔐 2.1. 인증 (Auth)

#### 로그인

* **`POST /auth/login`**
* **Req**: `{ "loginId": "user123", "password": "password123" }`
* **Res**: `200 OK`
* Body: `{ "data": "eyJhbG..." }` (Access Token)
* Cookie: `refresh_token` (자동 세팅됨, HttpOnly)



#### 로그아웃

* **`POST /auth/logout`**
* **Req**: (인증 헤더 및 쿠키 포함)
* **Res**: `204 No Content` (쿠키 삭제됨)

#### 토큰 갱신 (리프레시)

* **`POST /auth/refresh`**
* **Req**: (refresh_token 쿠키 포함)
* **Res**: `200 OK`
* Body: `{ "data": "새로운 AccessToken" }`



### 👤 2.2. 유저 (User)

#### 회원가입

* **`POST /users`**
* **Req**: `{ "loginId": "user123", "password": "password", "nickname": "야추왕" }`
* **Res**: `201 Created` / `{ "data": 1 }` (생성된 유저 ID)

#### 유저 조회

* **`GET /users/{nickname}`**
* **Res**: `200 OK` / `{ "nickname": "야추왕" }`

#### 회원 탈퇴

* **`DELETE /users/me`** (Auth 필요)
* **Res**: `204 No Content`

### 🏠 2.3. 게임 방 대기실 (Game Room)

#### 방 생성

* **`POST /rooms`** (Auth 필요)
* **Req**: `{ "roomName": "초보만 오세요" }`
* **Res**: `201 Created` / `{ "data": 1 }` (생성된 방 ID)

#### 방 목록 조회 (무한 스크롤 용)

* **`GET /rooms?page=0&size=15&sort=id,desc`**
* **Res**: `200 OK` (Spring `Slice` 객체 반환)
```json
{
  "content": [
    { "id": 1, "roomName": "초보만", "hostNickName": "야추왕", "participatedUsers": 1 }
  ],
  "pageable": { ... },
  "last": true,
  "empty": false
}

```



#### 특정 방 상세 정보 조회

* **`GET /rooms/{roomId}`**
* **Res**: `200 OK`
```json
{
  "id": 1,
  "roomName": "초보만 오세요",
  "host": { "userId": 1, "userNick": "야추왕", "isReady": true },
  "participatedUsers": 2,
  "participants": [
    { "userId": 1, "userNick": "야추왕", "isReady": true },
    { "userId": 2, "userNick": "도전자", "isReady": false }
  ]
}

```



---

## 3. WebSocket (STOMP) 명세

### 🔌 3.1. 연결 및 구독 (Connection & Subscribe)

* **Endpoint**: `/ws-stomp` (SockJS 지원)
* **Connect Header**: `{"Authorization": "Bearer <AccessToken>"}`
* **구독(Subscribe) 해야 할 주소**:
1. **해당 방 정보 수신**: `/sub/rooms/{roomId}` (방 안에서 일어나는 모든 상태 변화 수신)
2. **개인 에러 수신**: `/user/queue/errors` (나에게만 뜨는 에러 경고용 - Ex. "차례가 아닙니다.")



### ✉️ 3.2. 서버에서 오는 응답(수신) 포맷 (`SocketResponse<T>`)

`/sub/rooms/...` 를 통해 들어오는 모든 메시지는 아래 포맷을 따릅니다. 프론트는 `type`을 보고 화면을 업데이트합니다.

```json
{
  "type": "ENTER | QUIT | START | TOGGLE_READY | ROLL | KEEP_TOGGLE | SELECT_SCORE | GAME_OVER | TIME_OUT",
  "data": { ... 상태 객체 ... }
}

```

### 🎮 3.3. 프론트엔드에서 서버로 보내는(발행) 액션 목록

> **주의:** 발송 시 목적지 주소 앞에는 항상 `/pub`이 붙습니다.

#### [대기실 액션]

* **방 입장**: `SEND /pub/rooms/{roomId}/enter` (Payload 없음)
* **방 퇴장**: `SEND /pub/rooms/{roomId}/leave` (Payload 없음)
* **준비 토글**: `SEND /pub/rooms/{roomId}/toggleReady` (Payload 없음)
* **게임 시작**: `SEND /pub/rooms/{roomId}/start` (Payload 없음, 방장만 가능)

#### [게임 플레이 액션]

모든 인게임 행동은 단일 주소(`action`)로 보내며, Payload의 `type` 필드로 동작을 구분합니다.

* **주소**: `SEND /pub/games/{roomId}/action`
* **포맷 (`GameAction` 객체)**:
```json
{
  "type": "ROLL | KEEP_TOGGLE | SELECT_SCORE",
  "roomId": 1,
  "keepIndices": [0, 2],      // 주사위 킵 할때만 사용 (배열 인덱스)
  "scoreCategory": "YACHT"    // 점수 기록 할때만 사용
}

```



**[게임 플레이 발송 예시]**

1. **주사위 굴리기**
```json
{ "type": "ROLL", "roomId": 1 }

```


2. **주사위 킵(토글)** (예: 1번째, 3번째 주사위 킵 상태 바꾸기)
```json
{ "type": "KEEP_TOGGLE", "roomId": 1, "keepIndices": [0, 2] }

```


3. **족보 점수 등록**
```json
{ "type": "SELECT_SCORE", "roomId": 1, "scoreCategory": "FULL_HOUSE" }

```



---

## 4. 참고: 핵심 데이터 모델 (프론트엔드용)

### 📊 `GameState` (게임 진행 중 계속 업데이트되어 내려오는 판때기 정보)

게임 시작(`START`) 후부터 매 턴마다 브로드캐스팅되는 메인 객체입니다.

```json
{
  "startedAt": "2026-02-21T19:30:00",
  "turnTimeoutTime": "2026-02-21T19:33:00", // (중요) 이 시간에서 현재 시간을 빼서 남은 타이머 계산
  "roomId": 1,
  "curTurnUserId": 5,           // 현재 턴인 유저의 ID
  "leftRollCnt": 2,             // 남은 주사위 굴리기 횟수 (3 -> 2 -> 1 -> 0)
  "round": 1,                   // 현재 라운드 (1~12)
  "turn": 0,                    // 턴 인덱스
  "turnList": [5, 8],           // 턴 순서를 나타내는 유저 ID 배열
  "dice": [3, 3, 6, 1, 2],      // 현재 주사위 눈금 (길이 5)
  "kept": [true, true, false, false, false], // 주사위 킵 상태 (길이 5)
  "scores": {                   // 유저별 점수판 객체 (키: userId 문자열)
    "5": {
      "ones": 3, "twos": null, "threes": null, // null은 아직 안 채운 칸
      "bonus": null, "upperScore": 3, "total": 3
    },
    "8": { ... }
  }
}

```

### 🏷️ `scoreCategory` 입력 가능 문자열 (족보 키 목록)

* `ONES`, `TWOS`, `THREES`, `FOURS`, `FIVES`, `SIXES`
* `CHOICE`
* `FOUR_OF_A_KIND`
* `FULL_HOUSE`
* `S_STRAIGHT` (스몰 스트레이트)
* `L_STRAIGHT` (라지 스트레이트)
* `YACHT`