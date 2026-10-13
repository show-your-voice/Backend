# AGENTS.md

## 프로젝트 개요

- 이름: 너목보
- 목적: 학교에서 생기는 불편 사항(시설 고장, 와이파이 불안정 등)을 웹에서 제보하고, 해결될 때까지의 과정을 누구나 확인할 수 있게 하는 서비스
- 주요 사용자
    - 일반 사용자: 로그인 후 불편 사항 제보, 처리 상태와 담당 선생님 확인
    - 선생님(TEACHER): 게시글 상태 변경, 담당 영역 관리, 교내 건물·장소 관리

## 기술 스택

- Language: Java 21, Lombok(보일러 플레이트 코드 방지용)
- Framework: Spring Boot 4.1.1 (Spring MVC)
- Security: Spring Security
- ORM: Spring Data JPA
- DB: MySQL 8.4
- Build: Gradle (Groovy DSL, `build.gradle`)

## 명령어

```bash
./gradlew build          # 빌드
./gradlew bootRun        # 로컬 실행
```

## 도메인 및 비즈니스 규칙

### 게시글 (불편 사항)

- 입력 항목: 제목, 내용, 개선방향, 사진, 카테고리, 위치(건물 + 세부 위치 좌표)
- 등록 시 서버가 자동으로 지정하는 값: 상태 `미해결`, 작성 시각, 작성자
- 입력값 검증은 서버에서 다시 한다. 제목·내용 누락, 글자 수 초과, 존재하지 않는 카테고리·건물은 거부하고 어떤 입력란이 틀렸는지 응답에 담는다.
- 목록 조회는 페이지 단위로 하고 전체 게시글 수를 같이 내려준다.
- 목록은 건물별, 상태별 필터링을 지원하고 두 조건을 같이 쓸 수 있다.
- 상세 조회는 전체 내용, 개선방향, 첨부 사진, 위치, 현재 상태, 담당 선생님 정보, 상태별 변경 시각을 내려준다.

### 처리 상태

- 상태는 `미해결 → 진행 중 → 해결 완료` 3단계다.
- 상태를 바꿀 때마다 변경 시각을 저장한다. 접수, 처리 시작, 해결 시각을 각각 확인할 수 있어야 한다.
- 상태 변경은 선생님만 할 수 있다. 일반 사용자의 요청은 서버에서 거부한다.

### 사진 첨부

- 사진은 게시글 등록 전에 먼저 업로드한다. 서버는 저장된 이미지 주소를 돌려주고, 게시글 등록 시 그 주소를 함께 저장해 게시글과 연결한다.
- 서버는 파일 형식과 용량을 검사해서 허용되지 않은 파일은 거부한다.

### 건물·장소 (교내 지도)

- 선생님이 건물·장소의 이름과 좌표를 등록, 수정, 삭제한다.
- 사용자 위치 선택 화면과 건물별 필터는 이 데이터를 서버에서 불러온다. 건물이 바뀌어도 코드 수정 없이 반영돼야 한다.
- 게시글이 연결된 건물은 삭제하지 못하게 서버에서 막는다.

### 담당 선생님 자동 지정

- 게시글 담당은 선생님(TEACHER) 계정이다. 별도의 담당자 데이터를 두지 않는다.
- 선생님은 `건물 + 카테고리` 조합별로 담당 선생님 계정을 미리 등록한다. (예: 기숙사 + 시설 → 시설 담당 선생님)
- 게시글이 등록되면 서버가 게시글의 건물과 카테고리로 담당 선생님을 자동 지정한다.
- 조건에 맞는 담당 선생님이 없으면 미지정으로 두고, 선생님이 직접 지정할 수 있다.
- 선생님은 로그인한 계정 기준으로 본인에게 지정된 게시글만 모아 볼 수 있고, 상태별로 걸러볼 수 있다.

### 메일 알림

- 새 게시글이 등록되면 자동 지정된 담당 선생님 계정의 이메일로 제목, 위치, 내용, 상세 페이지 링크를 담은 메일을 보낸다.
- 메일 발송은 비동기로 처리해서 게시글 등록 응답을 늦추지 않는다.
- 메일 발송이 실패해도 게시글 등록은 정상 완료돼야 한다.

### 로그인 / 권한

- 일반 사용자와 선생님 모두 로그인 기능이 있다. 역할은 일반 사용자(USER)와 선생님(TEACHER)으로 구분한다.
- 로그인은 세션 방식을 사용한다.
- 게시글 작성자는 로그인한 사용자 정보로 서버가 지정한다.
- 선생님 기능은 선생님 권한이 있는 계정만 사용할 수 있다.
- 서버는 요청마다 로그인 정보를 확인하고, 권한이 없는 요청은 차단한다. 화면에서 메뉴를 숨기는 것에 기대지 않는다.

## 패키지 구조

```
src/main/java/{base.package}/
├── domain/
│   └── {도메인}/
│       ├── controller/  # Controller
│       │   └── dto/     # Request/Response DTO
│       ├── service/     # 비즈니스 로직
│       ├── domain/      # Entity, Enum, Repository
│       └── exception/   # 도메인 예외
└── global/
    ├── config/          # 설정 (Security, JPA 등)
    ├── error/           # 전역 예외 처리, ErrorCode
    ├── security/        # 인증/인가
    └── entity/          # BaseTimeEntity 등 공통 엔티티
```

- 새 도메인을 추가할 때도 `controller(dto) / service / domain / exception` 구조를 그대로 따른다.
- 의존성 방향은 항상 `controller → service → repository`로 간다.
- DTO 이름은 `{동작}{도메인}Request`, `{도메인}{용도}Response` 형식으로 짓는다 (예: `CreatePostRequest`, `PostDetailResponse`).
- 다른 도메인의 Repository를 직접 참조하지 않는다. 필요하면 해당 도메인의 Service를 거친다.

## 코딩 규칙

### Java

- DTO는 `record`로 작성한다.
- `Optional`은 반환 타입으로만 쓴다. 필드, 파라미터에 쓰지 않는다.
- 조회 실패는 `orElseThrow(XxxNotFoundException::new)`로 도메인 예외를 던진다. `Optional.get()` 금지.
- 의미 있는 상태 값은 `enum`으로 표현한다.

### Lombok

- Lombok은 필수로 사용한다. 생성자, getter 등 보일러플레이트를 직접 작성하지 않는다.
- Entity에 `@Setter`, `@Data` 금지.
- Entity에 `@ToString`, `@EqualsAndHashCode`를 붙이지 않는다 (연관관계 순환 참조, 지연 로딩 문제).
- Entity 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- 객체 생성은 `@Builder` 또는 정적 팩토리 메서드로 한다.

### JPA / Entity

- 상태 변경은 의미 있는 이름의 메서드로 한다 (`changeTitle()`, `cancel()`).
- 상태 변경이나 도메인 메서드를 활용할 수 있는 부분은 최대로 사용한다.
- 연관관계는 기본 `FetchType.LAZY`. `@ManyToOne`, `@OneToOne`은 기본값이 EAGER라 명시한다.
- 양방향 연관관계는 꼭 필요할 때만 쓴다.
- N+1 발생 가능 지점은 fetch join(`@Query`) 또는 `@EntityGraph`로 해결한다.

### Service / 트랜잭션

- 클래스 단위로 `@Transactional(readOnly = true)`를 기본으로 하고, 쓰기 메서드에만 `@Transactional`을 붙인다.
- 의존성 주입은 생성자 주입만 사용한다 (`@RequiredArgsConstructor` + `private final`).
- Controller에서 Entity를 직접 반환하지 않는다. 항상 Response DTO로 변환한다.

### 예외 처리

- 모든 비즈니스 예외는 공통 `BusinessException(ErrorCode)`를 상속한다.
- `ErrorCode`는 HTTP status, 코드, 메시지를 가진 enum으로 관리한다.
- 전역 처리는 `@RestControllerAdvice` 한 곳에서 한다.

### API

- REST 규칙: 리소스는 복수형 명사, 동작은 HTTP 메서드로 표현한다.
- 요청 검증은 `@Valid` + Bean Validation.

## Git

- 브랜치: `feature/{이슈번호}-{설명}`, `fix/...`, `refactor/...`
- 커밋: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:` 접두사 사용
- `main` 직접 push 금지. 기능별 브랜치에서 작업하고 Pull Request 코드 리뷰를 거쳐 병합한다.

## 하지 말 것

- `application-*.yml`의 비밀값(DB 비밀번호, 키) 커밋 금지. 환경변수 또는 `.env`로 관리한다.
- 사용자가 요청하지 않은 부분은 수정하지 않는다. 요청 범위 밖의 코드, 설정, 요구사항을 임의로 바꾸거나 추가하지 않는다.
- 요청받지 않은 대규모 리팩토링 금지.
- 새 의존성 추가 전에 먼저 이유를 설명하고 확인받는다.