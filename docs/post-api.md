# 불편사항 게시물 API

모든 API는 로그인 세션이 필요하다. 프론트 요청에 `credentials: 'include'`를 설정한다.

## 호출 순서

1. `GET /api/places`로 DB에 등록된 장소(층) 목록을 조회하고 `placeId`를 선택한다. 응답의 `imageUrl`로 그 층의 이미지를 띄운다.
2. 사용자가 이미지 위에서 찍은 위치를 이미지 크기 대비 비율(0~1)로 바꿔 `posX`, `posY`에 담는다.
3. `POST /api/posts`로 제목·내용·카테고리·장소·좌표와 선택 항목인 개선방향을 전송한다.
4. 생성 응답의 `id`로 `GET /api/posts/{id}`를 호출하면 같은 구조의 상세 정보를 받는다.
5. `GET /api/posts`로 게시물 목록을, `DELETE /api/posts/{id}`로 게시물을 삭제한다.

사진 업로드와 게시물 생성 시 사진 연결은 다른 팀원이 담당하며 현재 API에서 보류한다. 상세 조회는 `tbl_post_image`에 저장된 사진 주소를 `imageUrls`로 내려준다.

## 게시물 생성

`POST /api/posts`, `Content-Type: application/json`

```json
{
  "title": "화장실 세면대 물이 안 나와요",
  "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
  "category": "FACILITY",
  "placeId": 1,
  "posX": 0.4215,
  "posY": 0.6530,
  "improvement": "수도꼭지를 교체해 주세요."
}
```

제목(1~100자), 내용(1~5000자), 카테고리, 장소, 좌표는 필수다. 제목과 내용에 공백만 입력할 수 없다. `posX`, `posY`는 0 이상 1 이하, 소수점 넷째 자리까지다. 개선방향은 최대 1000자이며 생략·null·공백 입력은 `null`로 저장한다.

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
  "posX": 0.4215,
  "posY": 0.6530,
  "author": { "id": 10, "loginId": "student01", "email": "student01@dsm.hs.kr", "name": "김학생", "role": "USER" },
  "assignee": { "id": 20, "loginId": "teacher01", "email": "teacher@dsm.hs.kr", "name": "이선생", "role": "TEACHER" },
  "status": { "code": "UNRESOLVED", "label": "미해결" },
  "imageUrls": [],
  "receivedAt": "2026-10-21T10:30:00.123456",
  "inProgressAt": null,
  "resolvedAt": null
}
```

작성자는 로그인한 사용자, 접수일은 서버 저장 시각, 최초 상태는 `UNRESOLVED`로 지정된다. 시간은 기존 코드의 `LocalDateTime` 형식이다. 처리 상태 코드는 `UNRESOLVED`, `IN_PROGRESS`, `RESOLVED`이며 상태 변경 API는 이번 구현 범위에 포함하지 않는다.

## 게시물 상세 조회

`GET /api/posts/{id}`는 생성 응답과 같은 구조를 반환한다. `imageUrls`는 `sort_order` 오름차순이며 사진이 없으면 빈 배열이다.

## 게시물 목록 조회

`GET /api/posts?placeId=1&status=UNRESOLVED`

`placeId`, `status`는 선택이며 함께 쓸 수 있다. 페이지네이션 없이 조건에 맞는 게시물 전체를 최신순으로 반환한다.

```json
[
  {
    "id": 1,
    "title": "화장실 세면대 물이 안 나와요",
    "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
    "status": { "code": "UNRESOLVED", "label": "미해결" },
    "authorName": "김학생",
    "placeName": "창의관 4층",
    "receivedAt": "2026-10-21T10:30:00.123456"
  }
]
```

## 게시물 삭제

`DELETE /api/posts/{id}`

작성자 본인 또는 선생님(`TEACHER`)만 삭제할 수 있다. 성공 시 `204 No Content`를 반환하고 첨부 사진 정보도 함께 삭제한다. 다른 사용자가 요청하면 `403 P002`를 반환한다.

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

## 오류

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | C001 | 입력값 오류 |
| 401 | A001 | 로그인 필요 |
| 403 | P002 | 게시물 삭제 권한 없음 |
| 404 | L001 | 존재하지 않는 장소 |
| 404 | P001 | 존재하지 않는 게시물 |
| 409 | L002 | 담당 규칙의 계정 역할 오류 |

입력란 검증 오류는 기존 `{ "code", "message", "errors": [{ "field", "reason" }] }` 형식이며 잘못된 카테고리는 `category`, 없는 장소는 `placeId`를 표시한다.
