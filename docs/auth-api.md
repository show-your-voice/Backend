# 인증 API

로그인 식별자는 아이디(`loginId`)다. 이메일은 가입할 때 함께 받으며 메일 알림에 쓴다.
세션 방식이므로 로그인 응답의 `JSESSIONID` 쿠키를 이후 요청에 그대로 보낸다.

## 아이디 규칙

- 영문 소문자와 숫자만, 4~20자 (`^[a-z0-9]{4,20}$`)
- 중복 불가 (`tbl_user.login_id` UNIQUE)

## API

| 메서드 | 경로 | 로그인 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/auth/login-ids/{loginId}/availability` | 불필요 | 아이디 사용 가능 여부 |
| POST | `/api/auth/email-verifications` | 불필요 | 이메일 인증번호 발송·재전송 |
| POST | `/api/auth/email-verifications/verify` | 불필요 | 이메일 인증번호 확인 |
| POST | `/api/auth/signup` | 불필요 | 회원가입 (역할은 항상 `USER`, 이메일 인증 필요) |
| POST | `/api/auth/login` | 불필요 | 로그인 |
| POST | `/api/auth/logout` | 필요 | 로그아웃 |
| GET | `/api/users/me` | 필요 | 내 이름 |
| GET | `/api/users/me/session` | 필요 | 내 이름과 세션 역할 |

### 아이디 사용 가능 여부

`GET /api/auth/login-ids/student01/availability` → `200`

```json
{ "available": true }
```

이미 가입된 아이디면 `false`다. 형식 검사는 하지 않으므로 형식은 회원가입 규칙으로 따로 확인한다.

### 이메일 인증번호 발송

`POST /api/auth/email-verifications` → `200`

```json
{ "email": "student01@dsm.hs.kr" }
```

```json
{ "expiresAt": "2026-11-23T16:15:28.34301", "expiresInSeconds": 180, "remainingAttempts": 5 }
```

- 6자리 숫자 인증번호를 메일로 보낸다. 유효시간은 3분, 입력 기회는 5번이다.
- 같은 이메일로 다시 요청하면(재전송) 새 번호로 바뀌고 유효시간·입력 기회·인증 여부가 처음부터 다시 시작된다. 이전 번호는 쓸 수 없다. 재전송 횟수 제한은 없다.
- `@dsm.hs.kr` 이메일만 받는다. 이미 가입된 이메일이면 `409 U001`이다.
- 메일을 보내지 못하면 `500 E006`이고 번호는 저장하지 않는다. 메일은 응답 전에 동기로 보낸다.

### 이메일 인증번호 확인

`POST /api/auth/email-verifications/verify` → `204`

```json
{ "email": "student01@dsm.hs.kr", "code": "482913" }
```

번호가 틀리면 `400 E002`와 함께 남은 기회를 돌려준다. 5번 모두 틀리면 남은 기회가 0이 되고, 그 뒤로는 맞는 번호를 넣어도 `429 E004`이므로 재전송해야 한다.

```json
{
  "code": "E002",
  "message": "인증번호가 일치하지 않습니다.",
  "errors": [{ "field": "code", "reason": "인증번호가 일치하지 않습니다." }],
  "remainingAttempts": 4
}
```

### 회원가입

```json
{
  "loginId": "student01",
  "email": "student01@dsm.hs.kr",
  "password": "LocalTest123!",
  "name": "김학생"
}
```

`201`로 사용자 정보를 돌려준다. 가입 후 자동 로그인은 하지 않는다.

가입하려면 같은 이메일의 인증을 30분 안에 마쳐야 한다. 인증하지 않았거나 인증한 지 30분이 지났으면 `400 E005`다. 가입에 쓴 인증은 지워지므로 같은 인증으로 다시 가입할 수 없다.

### 로그인

```json
{ "loginId": "student01", "password": "LocalTest123!" }
```

### 사용자 정보 응답

회원가입·로그인이 같은 형식을 쓴다.

```json
{ "id": 10, "loginId": "student01", "email": "student01@dsm.hs.kr", "name": "김학생", "role": "USER" }
```

### 내 이름

`GET /api/users/me` → `200`

로그인한 사용자의 이름만 돌려준다. 로그인하지 않으면 `401 A001`이다.

```json
{ "name": "김학생" }
```

### 현재 세션

`GET /api/users/me/session` → `200`

로그인한 사용자의 이름과 현재 세션의 역할(`USER` 또는 `TEACHER`)을 돌려준다.
로그인하지 않으면 `401 A001`이다.

```json
{ "name": "이선생", "role": "TEACHER" }
```

## 오류

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | `C001` | 입력값 검증 실패. `errors[].field`에 필드 이름 |
| 401 | `A003` | 아이디 또는 비밀번호 불일치 |
| 409 | `U001` | 이미 사용 중인 이메일 |
| 409 | `U003` | 이미 사용 중인 아이디 |
| 400 | `E001` | 인증번호를 요청하지 않은 이메일 |
| 400 | `E002` | 인증번호 불일치. `remainingAttempts`에 남은 기회 |
| 400 | `E003` | 인증번호 유효시간(3분) 지남 |
| 429 | `E004` | 입력 기회 5번 모두 사용. 재전송 필요 |
| 400 | `E005` | 이메일 인증 없이 가입 시도, 또는 인증 후 30분 지남 |
| 500 | `E006` | 인증번호 메일 발송 실패 |

## 선생님 계정 등록

선생님 계정은 API로 만들지 않고 DB에 직접 넣는다. 비밀번호는 BCrypt 해시를 넣는다.

```sql
INSERT INTO tbl_user (login_id, email, password, name, role, created_at, updated_at)
VALUES ('teacher01', 'teacher@dsm.hs.kr', '{BCrypt 해시}', '이선생', 'TEACHER', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
```

## 기존 DB 반영

`login_id`는 NOT NULL·UNIQUE라 `ddl-auto: update`만으로는 기존 사용자 행이 있는 DB에 추가되지 않는다.
개발 DB는 사용자 데이터를 비우고 다시 만든다. 사용자를 참조하는 게시물·담당 규칙도 함께 지워진다.

```sql
DROP TABLE IF EXISTS tbl_post_image, tbl_post, tbl_assignment_rule, tbl_user;
```

애플리케이션을 다시 실행하면 테이블이 새로 만들어진다. 이후 선생님 계정을 다시 넣는다.
