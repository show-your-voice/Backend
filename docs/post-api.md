# 불편사항 게시물 API

모든 API는 로그인 세션이 필요하다. 프론트 요청에 `credentials: 'include'`를 설정한다.

## 호출 순서

1. `GET /api/places`로 DB에 등록된 장소(층) 목록을 조회하고 `placeId`를 선택한다. 응답의 `imageUrl`로 그 층의 이미지를 띄운다.
2. `GET /api/rooms`에서 선택한 실의 `id`를 `roomId`로 사용한다. 이미지 위에서 찍은 위치를 이미지 크기 대비 비율(0~1)로 바꿔 `posX`, `posY`에 담는다.
3. 사진을 첨부하려면 먼저 `POST /api/images/posts`로 한 장씩 올리고 응답의 `imageUrl`을 모은다. ([사진 첨부 API](image-api.md))
4. `POST /api/posts`로 제목·내용·카테고리·실 ID·좌표와 선택 항목인 개선방향, 사진 주소(`imageUrls`)를 전송한다.
5. 생성 응답의 `id`로 `GET /api/posts/{id}`를 호출하면 같은 구조의 상세 정보를 받는다.
6. `GET /api/posts`로 게시물 목록을, `DELETE /api/posts/{id}`로 게시물을 삭제한다.

게시물 생성 시 보낸 사진 주소는 `tbl_post_image`에 보낸 순서대로 저장되고, 상세 조회의 `imageUrls`로 내려준다.

## 게시물 생성

`POST /api/posts`, `Content-Type: application/json`

```json
{
  "title": "화장실 세면대 물이 안 나와요",
  "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
  "category": "FACILITY",
  "roomId": 3,
  "posX": 0.4215,
  "posY": 0.6530,
  "improvement": "수도꼭지를 교체해 주세요.",
  "imageUrls": ["/images/posts/3f1c2a9e-7b4d-4e8a-9c21-7d4e5f6a8b90.jpg"]
}
```

제목(1~100자), 내용(1~5000자), 카테고리, 실 ID, 좌표는 필수다. 제목과 내용에 공백만 입력할 수 없다. `posX`, `posY`는 0 이상 1 이하, 소수점 넷째 자리까지다. 개선방향은 최대 1000자이며 생략·null·공백 입력은 `null`로 저장한다. `imageUrls`는 선택이며 최대 5장이다. 사진 업로드 API가 돌려준 게시글 사진 주소(`/images/posts/...`)만 받고, 서버에 없는 사진·외부 주소·장소 사진 주소는 `400 I004`로 거부한다.

| 카테고리 코드 | 표시명 |
| --- | --- |
| FACILITY | 시설 |
| NETWORK | 네트워크 |
| ENVIRONMENT | 환경·청소 |
| ETC | 기타 |

성공 시 `201 Created`, `Location: /api/posts/{id}`와 상세 정보를 반환한다.

```json
{
  "id": 1,
  "title": "화장실 세면대 물이 안 나와요",
  "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
  "improvement": "수도꼭지를 교체해 주세요.",
  "category": { "code": "FACILITY", "label": "시설" },
  "place": { "id": 1, "name": "창의관 4층", "imageUrl": "https://example.com/places/creative-4f.png" },
  "room": { "id": 3, "name": "화장실", "placeId": 1, "placeName": "창의관 4층" },
  "posX": 0.4215,
  "posY": 0.6530,
  "author": { "id": 10, "loginId": "student01", "email": "student01@dsm.hs.kr", "name": "김학생", "role": "USER" },
  "assignee": { "id": 20, "loginId": "teacher01", "email": "teacher@dsm.hs.kr", "name": "이선생", "role": "TEACHER" },
  "coAssignees": [],
  "status": { "code": "UNRESOLVED", "label": "미해결" },
  "canEdit": true,
  "canChangeStatus": false,
  "imageUrls": [],
  "receivedAt": "2026-10-21T10:30:00.123456",
  "inProgressAt": null,
  "resolvedAt": null
}
```

작성자는 로그인한 사용자, 접수일은 서버 저장 시각, 최초 상태는 `UNRESOLVED`로 지정된다. 시간은 기존 코드의 `LocalDateTime` 형식이다. 처리 상태 코드는 `UNRESOLVED`, `IN_PROGRESS`, `RESOLVED`이며 상태 변경 API는 이번 구현 범위에 포함하지 않는다.

## 게시물 상세 조회

`GET /api/posts/{id}`는 생성 응답과 같은 구조를 반환한다. `imageUrls`는 `sort_order` 오름차순이며 사진이 없으면 빈 배열이다.

`assignee`는 자동 지정된 기존 담당자이며 `coAssignees`는 추가된 공동 담당자 목록이다.
공동 담당자가 없으면 빈 배열이고, 추가 순서대로 `UserInfoResponse` 구조를 반환한다.

## 게시물 수정

`PUT /api/posts/{id}`, `Content-Type: application/json`

상세 응답의 `canEdit`는 현재 로그인한 사용자가 작성자일 때만 `true`다. 수정 요청은 생성과 같은 입력 형식으로 편집할 항목의 최종 값을 모두 보낸다. 성공하면 `200 OK`와 갱신된 상세 정보를 반환한다.

- 사진 목록에는 유지할 기존 주소와 새로 업로드한 주소를 표시 순서대로 보낸다. `imageUrls`를 생략하거나 빈 배열 또는 `null`로 보내면 첨부가 모두 해제된다.
- 실이나 카테고리를 바꾸면 해당 조합의 규칙으로 기본 담당자를 다시 지정한다. 해당 규칙이 없으면 미지정으로 바뀐다. 공동 담당자가 새 기본 담당자가 되면 공동 담당 목록에서 제외한다.
- 수정해도 작성자, 접수 시각, 처리 상태와 상태별 시각은 유지된다.

## 공동 담당자 추가

`POST /api/posts/{postId}/co-assignees`, `Content-Type: application/json`

```json
{ "managerId": 21 }
```

로그인한 선생님(`TEACHER`)이 `GET /api/managers`에서 고른 선생님 ID를 보낸다.
성공 시 `200 OK`와 갱신된 게시물 상세를 반환한다. 기존 `assignee`는 유지되며
`coAssignees`에 선택한 선생님이 추가된다. `managerId`는 필수 양수다.
기존 담당자나 이미 추가된 공동 담당자는 다시 추가할 수 없다.

관계는 `tbl_post_co_assignee`에 저장하고 `(post_id, manager_id)` 유니크 제약으로
중복을 방지한다. 같은 신고의 추가와 삭제는 신고 행 잠금으로 직렬화한다.
스키마 자동 업데이트를 사용하지 않는 배포 환경은 이 엔티티에 대응하는 테이블과
외래키·유니크 제약을 먼저 적용해야 한다.

## 게시물 목록 조회

`GET /api/posts?placeId=1&status=UNRESOLVED&page=0&size=20`

`placeId`, `status`는 선택이며 함께 쓸 수 있다. 조건에 맞는 게시물을 최신순으로 한 페이지씩 반환한다.

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | `0` | 페이지 번호. 0부터 시작하며, 음수는 0으로 처리한다. |
| `size` | `20` | 한 페이지의 게시물 수. 최대 2000이다. |

`totalCount`는 조건에 맞는 전체 게시물 수, `totalPages`는 전체 페이지 수다. 마지막 페이지를 넘는 `page`를 요청하면 `posts`가 빈 배열이다.

```json
{
  "posts": [
    {
      "id": 1,
      "title": "화장실 세면대 물이 안 나와요",
      "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
      "status": { "code": "UNRESOLVED", "label": "미해결" },
      "authorName": "김학생",
      "placeName": "창의관 4층",
      "receivedAt": "2026-10-21T10:30:00.123456"
    }
  ],
  "page": 0,
  "size": 20,
  "totalCount": 1,
  "totalPages": 1
}
```

## 내 담당 게시물 목록 조회

`GET /api/posts/assigned?status=IN_PROGRESS`

로그인한 선생님(`TEACHER`)이 기본 담당자이거나 공동 담당자인 게시물만 최신순으로 조회한다.
담당자 ID는 로그인 세션에서 확인하고 요청 파라미터로 받지 않는다.
페이지 없이 배열로 반환하고, 각 항목은 전체 게시물 목록의 `posts` 항목과 같은 형식이다. 한 게시물은 한 번만 포함된다.
`status`는 선택이고, 생략하면 세 상태를 모두 조회한다. 해당 게시물이 없으면 빈 배열을 반환한다.
비로그인 요청은 `401 A001`, 일반 사용자 요청은 `403 A002`를 받는다.

## 게시물 삭제

`DELETE /api/posts/{id}`

작성자 본인 또는 선생님(`TEACHER`)만 삭제할 수 있다. 성공 시 `204 No Content`를 반환하고 첨부 사진 정보와 공동 담당자 관계도 함께 삭제한다. 선생님 계정은 삭제하지 않는다. 다른 사용자가 요청하면 `403 P002`를 반환한다.

## 담당 규칙 DB 등록

`tbl_assignment_rule`에 `장소 + 카테고리` 조합별로 `TEACHER` 계정을 연결한다. 게시물이 등록되면 게시물의 장소와 카테고리가 같은 규칙의 선생님을 담당자로 지정한다. 같은 조합의 규칙이 여러 개면 먼저 등록된(id가 가장 작은) 규칙을 쓴다. 규칙이 없으면 `assignee: null`로 생성한다. 규칙의 선생님 계정이 `TEACHER`가 아니면 `409 L002`로 거부한다. 게시물 생성 당시의 담당자를 저장하므로 이후 규칙이 바뀌어도 기존 게시물 담당자는 유지된다.

```sql
INSERT INTO tbl_place (name, image_url, created_at, updated_at)
VALUES ('창의관 4층', 'https://example.com/places/creative-4f.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
SET @place_id = LAST_INSERT_ID();

SELECT id, login_id, email, name FROM tbl_user WHERE role = 'TEACHER';
SET @teacher_id = 20;

INSERT INTO tbl_assignment_rule (place_id, teacher_id, category, created_at, updated_at)
SELECT @place_id, id, 'FACILITY', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM tbl_user WHERE id = @teacher_id AND role = 'TEACHER';
```

`@teacher_id`에는 실제 DB의 선생님 ID를 넣는다. 장소와 담당 규칙의 관리 API는 이번 구현 범위에 포함하지 않는다.

## 담당 선생님 메일 알림

게시물이 등록되고 담당 선생님이 자동 지정되면, 그 선생님 계정의 이메일로 제목·위치(장소, 실)·내용·관리자 상세 페이지 링크(`{FRONTEND_URL}/manager/reports/{id}`)를 담은 메일을 보낸다.

- 게시물 저장이 커밋된 뒤 별도 스레드에서 보내므로 등록 응답을 기다리게 하지 않는다.
- 담당 선생님이 없거나 등록이 실패하면 메일을 보내지 않는다.
- 메일 발송이 실패해도 게시물 등록은 그대로 완료되고, 서버 로그에 경고만 남긴다. 재발송은 하지 않는다.

SMTP 정보와 프론트 주소는 환경변수로 넣는다. 비밀번호는 커밋하지 않는다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP 서버 |
| `MAIL_PORT` | `587` | SMTP 포트 (STARTTLS) |
| `MAIL_USERNAME` | 없음 | SMTP 계정. 보내는 사람 주소로도 쓴다 |
| `MAIL_PASSWORD` | 없음 | SMTP 비밀번호 (Gmail은 앱 비밀번호) |
| `FRONTEND_URL` | `http://localhost:5173` | 메일 속 상세 페이지 링크의 프론트 주소 |

## 오류

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | C001 | 입력값 오류 (사진 6장 이상 포함) |
| 400 | I004 | 업로드하지 않은 사진 주소 |
| 401 | A001 | 로그인 필요 |
| 403 | P007 | 작성자가 아닌 사용자의 수정 요청 |
| 403 | P002 | 게시물 삭제 권한 없음 |
| 403 | P003 | 공동 담당자 추가 권한 없음 |
| 404 | U002 | 존재하지 않는 공동 담당자 계정 |
| 404 | R001 | 존재하지 않는 실 |
| 404 | P001 | 존재하지 않는 게시물 |
| 409 | L002 | 담당 규칙의 계정 역할 오류 |
| 409 | P004 | 공동 담당자로 학생 계정을 선택함 |
| 409 | P005 | 기존 담당자 또는 이미 추가된 공동 담당자 |

입력란 검증 오류는 기존 `{ "code", "message", "errors": [{ "field", "reason" }] }` 형식이며 잘못된 카테고리는 `category`, 없는 실은 `roomId`, 사진 개수 초과나 잘못된 사진 주소는 `imageUrls`를 표시한다.
