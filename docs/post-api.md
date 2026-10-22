# 불편사항 게시물 API

모든 API는 로그인 세션이 필요하다. 프론트 요청에 `credentials: 'include'`를 설정한다.

## 호출 순서

1. `GET /api/places`로 DB에 등록된 장소 목록을 조회하고 `placeId`를 선택한다.
2. `POST /api/posts`로 제목·내용·카테고리·장소와 선택 항목인 개선방향을 전송한다.
3. 생성 응답의 `id`로 `GET /api/posts/{id}`를 호출하면 같은 구조의 상세 정보를 받는다.

사진 첨부는 다른 팀원이 담당하며 현재 API에서 보류한다. 사진 관련 요청·응답 필드와 업로드 기능은 포함하지 않는다.

## 게시물 생성

`POST /api/posts`, `Content-Type: application/json`

```json
{
  "title": "화장실 세면대 물이 안 나와요",
  "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
  "category": "FACILITY",
  "placeId": 1,
  "improvement": "수도꼭지를 교체해 주세요."
}
```

제목(1~100자), 내용(1~5000자), 카테고리, 장소는 필수다. 제목과 내용에 공백만 입력할 수 없다. 개선방향은 최대 1000자이며 생략·null·공백 입력은 `null`로 저장한다.

| 카테고리 코드 | 표시명 |
| --- | --- |
| FACILITY | 시설 고장 |
| NETWORK | 네트워크 |
| CLEANING | 청소 |
| OTHER | 기타 |

성공 시 `201 Created`, `Location: /api/posts/{id}`와 상세 정보를 반환한다.

```json
{
  "id": 1,
  "title": "화장실 세면대 물이 안 나와요",
  "content": "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.",
  "improvement": "수도꼭지를 교체해 주세요.",
  "category": { "code": "FACILITY", "label": "시설 고장" },
  "place": { "id": 1, "buildingName": "창의관", "floor": 4, "name": "화장실(남)" },
  "reporter": { "id": 10, "email": "student01@dsm.hs.kr", "name": "김학생", "role": "USER" },
  "assignedTeacher": { "id": 20, "email": "teacher@dsm.hs.kr", "name": "이선생", "role": "TEACHER" },
  "status": { "code": "RECEIVED", "label": "접수" },
  "receivedAt": "2026-10-21T10:30:00.123456",
  "processingStartedAt": null,
  "resolvedAt": null
}
```

신고자는 로그인한 사용자, 접수일은 서버 저장 시각, 최초 상태는 `RECEIVED`로 지정된다. 시간은 기존 코드의 `LocalDateTime` 형식이다. 처리 상태 코드는 `RECEIVED`, `IN_PROGRESS`, `COMPLETED`이며 상태 변경 API는 이번 구현 범위에 포함하지 않는다.

## 장소와 담당 선생님 DB 매핑

`tbl_place_teacher_assignment`에 장소별로 한 명의 `TEACHER` 계정을 연결한다. 카테고리와 무관하게 장소만으로 담당자를 결정한다. 매핑이 없으면 `assignedTeacher: null`로 생성한다. 매핑 대상이 `TEACHER`가 아니면 `409 L002`로 거부한다. 게시물 생성 당시의 담당자를 저장하므로 이후 매핑이 바뀌어도 기존 게시물 담당자는 유지된다.

```sql
INSERT INTO tbl_place (building_name, floor, name, created_at, updated_at)
VALUES ('창의관', 4, '화장실(남)', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
SET @place_id = LAST_INSERT_ID();

SELECT id, email, name FROM tbl_user WHERE role = 'TEACHER';
SET @teacher_id = 20;

INSERT INTO tbl_place_teacher_assignment (place_id, teacher_id, created_at, updated_at)
SELECT @place_id, id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM tbl_user WHERE id = @teacher_id AND role = 'TEACHER';
```

`@teacher_id`에는 실제 DB의 선생님 ID를 넣는다. 장소와 매핑의 관리 API는 이번 구현 범위에 포함하지 않는다.

## 오류

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | C001 | 입력값 오류 |
| 401 | A001 | 로그인 필요 |
| 404 | L001 | 존재하지 않는 장소 |
| 404 | P001 | 존재하지 않는 게시물 |
| 409 | L002 | 담당자 매핑의 계정 역할 오류 |

입력란 검증 오류는 기존 `{ "code", "message", "errors": [{ "field", "reason" }] }` 형식이며 잘못된 카테고리는 `category`, 없는 장소는 `placeId`를 표시한다.
