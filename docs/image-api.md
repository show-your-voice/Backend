# 사진 첨부 API

사진 파일을 서버에 올리고, 저장된 사진 주소를 돌려받는다. 게시글이나 장소(층 지도)에 사진을 연결하는 기능은 이 API에 포함하지 않으며, 그 기능을 만들 때 여기서 받은 `imageUrl`을 저장한다.

모든 요청은 로그인 세션이 필요하다. 프론트 요청에 `credentials: 'include'`를 설정한다. 로그인하지 않으면 `401 A001`을 반환한다.

## 업로드

| 용도 | 요청 | 권한 | 저장 폴더 |
| --- | --- | --- | --- |
| 게시글 사진 | `POST /api/images/posts` | 로그인한 모든 사용자 | `posts/` |
| 장소(층 지도) 사진 | `POST /api/images/places` | 선생님(`TEACHER`)만 | `places/` |

`Content-Type: multipart/form-data`, 파일 파트 이름은 `file`이다. 한 번에 사진 한 장을 올린다.

```js
const form = new FormData();
form.append('file', file);
const res = await fetch('/api/images/posts', { method: 'POST', body: form, credentials: 'include' });
```

성공 시 `201 Created`와 사진 주소를 반환한다.

```json
{ "imageUrl": "/images/posts/3f1c2a9e-7b4d-4e8a-9c21-7d4e5f6a8b90.jpg" }
```

- 허용 형식은 jpg, png, webp이고 용량은 5MB 이하다.
- 형식은 파일 이름이나 `Content-Type`이 아니라 파일 내용(앞부분 시그니처)으로 판별한다. 확장자만 바꾼 파일은 거부한다.
- 원본 파일 이름은 쓰지 않고, 임의의 UUID에 판별한 형식의 확장자를 붙여 저장한다.

## 사진 조회

`GET {imageUrl}` (예: `GET /images/posts/3f1c2a9e-….jpg`)

로그인한 사용자만 볼 수 있다. `<img src>`로 쓸 때도 세션 쿠키가 함께 전송되어야 한다. 없는 사진은 `404 C002`를 반환한다.

## 오류

| HTTP | 코드 | 의미 | `errors[0].field` |
| --- | --- | --- | --- |
| 400 | I001 | jpg, png, webp가 아니거나 빈 파일 | `file` |
| 400 | C001 | `file` 파트 없음 | `file` |
| 401 | A001 | 로그인 필요 | |
| 403 | A002 | 선생님이 아닌 사용자의 장소 사진 업로드 | |
| 413 | I002 | 5MB 초과 | `file` |
| 500 | I003 | 서버에 파일 저장 실패 | |

## 서버 설정

사진은 서버 로컬 폴더에 저장한다. 기본 위치는 실행 위치 기준 `uploads/`이며 환경변수 `IMAGE_UPLOAD_DIR`로 바꿀 수 있다. `uploads/`는 git에 올리지 않는다.
