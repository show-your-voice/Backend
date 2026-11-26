# 담당자 정보 조회 API

`GET /api/managers`

로그인한 학생(`USER`)과 선생님(`TEACHER`)이 조회할 수 있다. 기존 세션 인증을 사용하므로 프론트 요청에 `credentials: 'include'`를 설정한다. 로그인하지 않으면 `401 Unauthorized`, 기존 `A001` 오류를 반환한다.

## 응답

성공 시 `200 OK`와 담당자 배열을 반환한다. 프론트의 `ManagerInfoPage`가 받는 `managers` 구조와 같다.

```json
[
  {
    "id": 20,
    "name": "이선생",
    "email": "lee@dsm.hs.kr",
    "locations": ["4층 독서실", "4층 화장실(남)"]
  },
  {
    "id": 21,
    "name": "정선생",
    "email": "jung@dsm.hs.kr",
    "locations": []
  }
]
```

- 담당자는 별도 엔티티가 아닌 `tbl_user`의 `TEACHER` 계정이다. 이름 오름차순, 동명이인은 ID 오름차순으로 반환한다. 학생 계정과 비밀번호는 응답에 포함하지 않는다.
- `locations`는 기존 `tbl_assignment_rule`에 연결된 `tbl_place.name`이다. 게시물 자동 배정과 동일하게 장소·카테고리별 ID가 가장 작은 규칙만 적용한다. 과거 게시물에 저장된 담당자로 목록을 구성하지 않는다.
- 장소 이름 오름차순으로 정렬하며, 한 담당자가 여러 카테고리 또는 같은 이름의 장소를 맡더라도 동일한 표시 이름은 한 번만 반환한다. 같은 장소를 카테고리별로 서로 다른 선생님이 맡으면 각 선생님의 목록에 표시한다.
- 담당 장소가 없는 선생님도 `locations: []`로 포함한다. 담당자 계정이 없으면 `[]`를 반환한다.
- 필터와 페이지네이션 없이 전체 목록을 조회한다. 담당자 수와 무관하게 계정과 담당 규칙을 두 번의 조회로 불러온다.

## 담당 규칙 관리

선생님 계정으로 로그인한 뒤 실과 카테고리 조합마다 담당 선생님 한 명을 지정한다. 실과 선생님 선택 목록은 각각 `GET /api/rooms`, `GET /api/managers`에서 조회한다.

| 동작 | 메서드와 경로 | 요청 본문 예시 | 성공 응답 |
| --- | --- | --- | --- |
| 목록 조회 | `GET /api/assignment-rules` | — | 200, 규칙 배열 |
| 등록 | `POST /api/assignment-rules` | `{"roomId":12,"category":"FACILITY","teacherId":20}` | 201, 등록된 규칙 |
| 담당자 변경 | `PUT /api/assignment-rules/{roomId}/{category}` | `{"teacherId":21}` | 200, 수정된 규칙 |
| 삭제 | `DELETE /api/assignment-rules/{roomId}/{category}` | — | 204 |

규칙 응답은 `roomId`, `roomName`, `placeName`, `category`, `teacherId`, `teacherName`을 포함한다. 카테고리는 `FACILITY`, `NETWORK`, `ENVIRONMENT`, `ETC`를 사용한다.

로그인하지 않으면 401, 학생 계정으로 요청하면 403을 반환한다. 중복 등록은 `409 L003`, 학생을 담당자로 지정하면 `409 L002`, 없는 규칙의 수정·삭제는 `404 L004`다. 과거에 중복 등록된 조합은 최초 규칙을 표시하며, 해당 조합을 삭제하면 기존 중복 규칙도 함께 제거한다.
