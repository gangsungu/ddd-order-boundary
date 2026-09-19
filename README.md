# ddd-order-boundary

주문 도메인의 **바운디드 컨텍스트와 서비스 경계**를 직접 설계하고, 그 설계를 Spring Boot 코드로 옮겨가는 학습용 프로젝트입니다.

인프런 [**15년차 개발자의 코드 리뷰와 함께하는 4주 실전 MSA 설계 챌린지**](https://www.inflearn.com/challenge/4-week-practical-ddd/dashboard?cid=343587) (2026.09.08 ~ 10.06) 참여 기록이며,
강의 예제 저장소는 [truthplumage/backend_public5](https://github.com/truthplumage/backend_public5) 입니다.

---

## 챌린지 커리큘럼

| 섹션 | 주제 | 미션 |
|---|---|---|
| 1 | 실전 주문 시스템 만들기 (기초 구조) — Spring Boot 환경 구축, 모놀리식 CRUD, DDD 기본 개념 | **주문 도메인의 MSA 서비스 경계 혼자 설계하기** |
| 2 | 헥사고날 아키텍처로 개선 (Ports & Adapters, Clean Architecture) | |
| 3 | 분산 시스템 설계 (CQRS, Event-Driven, ACL, Saga) | |
| 4 | Kafka & gRPC 실습 | |

현재 진행 상황: **섹션 1 완료** — 설계 문서 + 상품 도메인 CRUD + 공통 예외 처리. 섹션 2(헥사고날 전환) 준비 중

---

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Web, Data JPA) |
| Database | H2 (개발·테스트 기본) · PostgreSQL (`postgres` 프로필) |
| Build | Gradle 9.7.1 (Wrapper) |
| 기타 | Lombok |

---

## 실행 방법

별도 DB 설치 없이 클론 후 바로 실행됩니다. 데이터는 `./data/devdb` 파일에 저장되어 재시작해도 유지되고, H2 콘솔은 <http://localhost:8080/h2-console> 에서 열 수 있습니다.

```bash
./gradlew bootRun
```

PostgreSQL 로 띄우려면 프로필을 전환합니다. 접속 정보는 `.env` 또는 환경 변수(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`)로 덮어쓸 수 있습니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

| 설정 파일 | 용도 |
|---|---|
| `src/main/resources/application.yml` | H2 파일 DB (기본), `ddl-auto: update` |
| `src/main/resources/application-postgres.yml` | PostgreSQL, `ddl-auto: none` |
| `src/test/resources/application.yml` | H2 인메모리, 테스트 전용 (개발 DB와 격리) |

H2 는 `MODE=PostgreSQL` 호환 모드로 동작합니다. 3주차 분산 시스템 단계에서 실제 PostgreSQL 이 필요해지면 프로필만 바꾸면 됩니다.

---

## 프로젝트 구조

```
src/main/java/com/roykhan/dddorderboundary
├── DddOrderBoundaryApplication.java
├── common
│   ├── config
│   │   ├── JacksonConfig.java           # ApiResponse 전용 직렬화 모듈 등록
│   │   └── JpaAuditingConfig.java
│   ├── controller
│   │   └── HealthController.java
│   ├── exception
│   │   ├── BaseErrorCode.java           # 에러 코드 계약 (상태/메시지/예외 생성)
│   │   ├── BusinessException.java       # 에러 코드를 실어 나르는 도메인 예외
│   │   ├── CommonErrorCode.java         # 공통 4xx/5xx 코드
│   │   ├── ProductErrorCode.java        # 상품 도메인 코드
│   │   └── GlobalExceptionHandler.java  # 예외 → ApiResponse 변환
│   └── response
│       ├── ApiResponse.java             # 모든 응답의 공통 규격
│       └── ApiResponseSerializer.java   # 성공 시 data 생략, 실패 시 유지
└── domain
    ├── base
    │   └── BaseEntity.java              # 공통 식별자 + 생성/수정 시각 감사(Auditing)
    ├── order
    │   └── Order.java                   # 아직 뼈대만 존재
    └── product
        ├── Product.java                 # 상품명, 설명, 가격
        ├── controller/ProductController.java
        ├── dto
        │   ├── ProductInfo.java         # 응답용
        │   └── ProductRegisterRequest.java  # 등록/수정 요청용 (검증 제약 포함)
        ├── repository/ProductRepository.java
        └── service/ProductService.java

docs/섹션1                                # 섹션 1 미션 산출물 (설계 문서)
```

섹션 2부터는 이 패키지 구조를 헥사고날(`presentation` / `application` / `domain` / `infrastructure`) 구조로 리팩터링할 예정입니다.

---

## 구현 현황

### 상품 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/product/{id}` | 상품 단건 조회 |
| `POST` | `/api/product` | 상품 등록 (이름 중복 불가) |
| `PUT` | `/api/product/{id}` | 상품 수정 |
| `DELETE` | `/api/product/{id}` | 상품 삭제 |
| `GET` | `/api/health` | 헬스 체크 |

### 응답 규격

성공과 실패 모두 `ApiResponse` 한 가지 규격을 따릅니다. 성공 응답은 `data` 가 없으면 키를 생략하고, 실패 응답은 클라이언트가 항상 읽을 수 있도록 `data` 키를 유지합니다.

```json
// 성공
{"success": true, "code": "success", "message": "상품을 조회하였습니다.", "data": { ... }}

// 실패
{"success": false, "code": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다.", "data": null}
```

### 에러 처리

도메인 예외는 `BusinessException` 에 에러 코드를 실어 던지고, `GlobalExceptionHandler` 가 상태 코드와 응답 본문으로 변환합니다. 스프링이 이미 올바른 상태 코드로 처리하던 예외들은 개별 핸들러로 받아 상태를 보존하며, `Exception` 핸들러는 예상하지 못한 오류 전용 최종 방어선으로 남겨둡니다.

| 상황 | 상태 | 코드 |
|---|---|---|
| 없는 상품 조회·수정·삭제 | 404 | `PRODUCT_NOT_FOUND` |
| 이름이 중복된 상품 등록 | 409 | `PRODUCT_ALREADY_EXIST` |
| 입력값 검증 실패 | 400 | `VALIDATION_FAILED` *(필드별 오류를 `data` 에 담음)* |
| 깨진 JSON, 파라미터 타입 불일치 | 400 | `INVALID_REQUEST` |
| 존재하지 않는 경로 | 404 | `NOT_FOUND` |
| 지원하지 않는 메서드 | 405 | `METHOD_NOT_ALLOWED` |
| 지원하지 않는 Content-Type | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| 그 외 모든 예외 | 500 | `INTERNAL_SERVER_ERROR` |

---

## 다음 작업

### 섹션 2 — 헥사고날 전환

- [ ] **주문 도메인 최소 구현** — `Order` / `OrderItem`, 주문 생성·조회·취소
      주문 시점의 상품명·단가를 스냅샷으로 복사 (설계 문서의 "시점이 중요한 값은 복사한다" 원칙)
- [ ] **패키지 구조를 헥사고날로 전환** — Ports & Adapters

> 주문을 먼저 만드는 이유: 주문 생성이 상품 컨텍스트를 조회해야 해서 출력 포트가 자연스럽게 필요해집니다.
> 상품 하나만으로는 포트가 리포지토리뿐이라 포트/어댑터 분리의 효과가 드러나지 않습니다.

### 전환 전 정리 대상

- [ ] `Product` 엔티티의 클래스 레벨 `@Setter` 제거 — 도메인 모델을 분리할 때 가장 먼저 걸리는 지점
- [ ] `jakarta.transaction.Transactional` → 스프링의 `@Transactional` (`readOnly` 사용 불가)
- [ ] `ProductService.findById` 에 읽기 전용 트랜잭션 적용
- [ ] `ProductErrorCode` 를 상품 도메인 패키지로 이동
- [ ] `GlobalExceptionHandler` 슬라이스 테스트 추가 (현재 회귀 방지 없음)
- [ ] 상품 목록 조회 엔드포인트 *(선택 — 주문 구현에는 불필요)*

### 섹션 3 이후

- 결제 · 정산 컨텍스트 — 외부 PG 연동과 배치라 Saga·보상 트랜잭션을 다루는 섹션 3에서 진행
- CQRS, Event-Driven, ACL / Kafka, gRPC

> 설계 문서의 상품 컨텍스트에는 판매상태·재고·판매자 ID 가 있지만 엔티티에는 반영하지 않았습니다.
> 모놀리식은 간단히 두고 아키텍처 전환에 집중하는 것이 섹션 2의 목적이라, 의도적으로 맞추지 않았습니다.

---

## 섹션 1 설계 문서

| 문서 | 내용 |
|---|---|
| [주문 시스템의 주요 기능과 도메인 분석](docs/섹션1/주문-시스템의-주요-기능과-도메인-분석.md) | 오픈마켓형 커머스 전제, 주문 성공/실패/취소/정산 도메인 이벤트 흐름 |
| [기능별 책임 분리](docs/섹션1/기능별-책임-분리.md) | 컨텍스트별 변경 이유·수명 주기·트랜잭션 범위 비교, 상품/재고 분리 가능성 |
| [서비스가 담당하는 데이터와 역할 정의](docs/섹션1/서비스가-담당하는-데이터와-역할-정의.md) | 데이터 소유권 원칙, ID 참조 vs 스냅샷 복사 기준 |
| [서비스 간 의존 관계를 고려하여 MSA 구조로 설계](docs/섹션1/서비스-간-의존-관계를-고려하여-MSA-구조로-설계.md) | 주문이 흐름을 주도하는 서비스 간 의존 구조 |

### 설계 요약

도메인은 **오픈마켓형 커머스**(여러 판매자 입점, 플랫폼이 중개 수수료 수취)를 전제로 4개 컨텍스트로 나눴습니다.

| 컨텍스트 | 소유 데이터 | 다른 컨텍스트의 참조 방식 |
|---|---|---|
| 주문 | 주문, 주문항목, 주문상태 | 주문ID |
| 상품 | 상품정보, 가격, 판매상태, 재고수량 | 상품ID |
| 결제 | 결제, 결제상태, PG 거래번호, 환불 | 결제ID |
| 정산 | 정산배치, 정산내역, 수수료, 지급상태 | 정산ID |

경계를 가른 기준은 세 가지입니다.

1. **하나의 데이터는 하나의 컨텍스트만 소유한다** — 쓰기 권한이 한 곳에만 존재하고, 나머지는 ID로만 참조합니다.
2. **시점이 중요한 값은 복사한다** — 상품명·단가는 상품 컨텍스트가 소유하지만, 프로모션 종료 후 과거 주문 금액이 달라지면 안 되므로 주문항목이 주문 시점 값을 스냅샷으로 갖습니다. (판매자 상호 → 주문, 주문 총액 → 결제, 거래액 → 정산도 같은 이유)
3. **변경 이유와 수명 주기가 다르면 나눈다** — 주문은 확정되면 상태 변화가 끝나지만, 정산은 주문 종료 시점부터 집계·지급 단계를 거치며 계속 변합니다. 환불이 발생해도 종료된 주문은 그대로이고 정산 금액만 바뀝니다.

재고는 주문마다 변경되어 쓰기 경합이 생기고 상품 정보는 읽기 위주라 성격이 다르지만, 현재 범위에서는 상품 컨텍스트에 함께 두고 경합이 문제가 되면 분리하기로 했습니다.

---

## 참고 자료

강의 예제 저장소 [truthplumage/backend_public5](https://github.com/truthplumage/backend_public5)의 학습 문서:

- [DDD 기본 개념](https://github.com/truthplumage/backend_public5/tree/dev/docs/DDD_기본개념) — Bounded Context, Hexagonal Ports & Adapters, Clean Architecture, CQRS, Event-Driven Integration, ACL, Saga & Process Manager
- [전략적 설계](https://github.com/truthplumage/backend_public5/blob/dev/docs/02_전략적_설계.md) / [전술적 설계](https://github.com/truthplumage/backend_public5/blob/dev/docs/03_전술적_설계.md)
- [계층형 아키텍처 비교](https://github.com/truthplumage/backend_public5/blob/dev/docs/04_계층형_아키텍처_비교.md) / [CQRS 및 기타 구현방식](https://github.com/truthplumage/backend_public5/blob/dev/docs/05_CQRS_및_기타_구현방식.md)
- [개발 환경 설정](https://github.com/truthplumage/backend_public5/tree/dev/docs/개발환경) — PostgreSQL, Kafka(Docker), k8s
