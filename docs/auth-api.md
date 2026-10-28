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
| POST | `/api/auth/signup` | 불필요 | 회원가입 (역할은 항상 `USER`) |
| POST | `/api/auth/login` | 불필요 | 로그인 |
| POST | `/api/auth/logout` | 필요 | 로그아웃 |
| GET | `/api/users/me` | 필요 | 내 정보 |

### 아이디 사용 가능 여부

`GET /api/auth/login-ids/student01/availability` → `200`

```json
{ "available": true }
```

이미 가입된 아이디면 `false`다. 형식 검사는 하지 않으므로 형식은 회원가입 규칙으로 따로 확인한다.

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

### 로그인

```json
{ "loginId": "student01", "password": "LocalTest123!" }
```

### 사용자 정보 응답

회원가입·로그인·내 정보가 같은 형식을 쓴다.

```json
{ "id": 10, "loginId": "student01", "email": "student01@dsm.hs.kr", "name": "김학생", "role": "USER" }
```

## 오류

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | `C001` | 입력값 검증 실패. `errors[].field`에 필드 이름 |
| 401 | `A003` | 아이디 또는 비밀번호 불일치 |
| 409 | `U001` | 이미 사용 중인 이메일 |
| 409 | `U003` | 이미 사용 중인 아이디 |

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
