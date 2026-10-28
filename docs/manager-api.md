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

## 프론트 연결

```js
const response = await fetch('http://localhost:8080/api/managers', {
  credentials: 'include',
})
if (!response.ok) throw new Error('담당자 정보를 불러오지 못했습니다.')
const managers = await response.json()
```

조회 결과를 `ManagerInfoPage`의 `managers` prop에 전달하면 된다. 현재 FE의 `/managers` 경로는 미리보기 데이터를 사용하며 이번 서버 구현에는 FE API 연동을 포함하지 않는다.

## 데이터 등록 기준

장소·카테고리별 담당 규칙은 [기존 게시물 API 문서](post-api.md#담당-규칙-db-등록)의 DB 등록 방법을 따른다. 화면 예시의 이름·이메일·방 이름을 서버에 고정하지 않는다. 현재 장소 모델은 층별 이미지도 관리하므로 실제 DB에 등록한 장소 이름을 그대로 표시한다. 세부 방을 구분하려면 해당 이름으로 등록된 장소와 담당 규칙이 필요하다.

기존 엔티티와 스키마를 사용하며, 담당자·장소·담당 규칙의 생성·수정 API는 이번 조회 API에 포함하지 않는다.
