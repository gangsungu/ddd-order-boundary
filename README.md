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

현재 진행 상황: **섹션 2 진행 중** — 주문 생성·조회·취소, 재고 예약과 재고 조회까지 구현했습니다. 목업 결제와 헥사고날 전환이 남았습니다

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

API 를 순서대로 찔러볼 수 있는 시나리오는 [`docs/섹션2-데모.http`](docs/섹션2-데모.http) 에 있습니다. IntelliJ HTTP Client 로 위에서부터 실행하면 상품 등록 → 주문 → 재고 예약 → 취소·복원 흐름을 그대로 따라갈 수 있습니다. 결제 구간은 아직 구현 전이라 동작하지 않습니다.

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
    │   ├── Order.java                   # 주문 애그리거트 루트 — 상태 전이와 총액 계산
    │   ├── OrderItem.java               # 주문 시점 상품명·단가 스냅샷
    │   ├── enums/OrderStatus.java
    │   ├── controller/OrderController.java
    │   ├── dto
    │   │   ├── CreateOrderRequest.java  # 생성 요청 (중첩 OrderLine)
    │   │   ├── OrderCreateInfo.java     # 생성 응답 (주문 ID)
    │   │   └── OrderInfo.java           # 조회 응답 (항목 내역 포함)
    │   ├── repository/OrderRepository.java
    │   └── service/OrderService.java
    ├── stock
    │   ├── Stock.java                   # 총 재고와 가용 수량, 낙관적 락
    │   ├── StockReservation.java        # 예약 주체·수량·만료 시각
    │   ├── enums
    │   │   ├── StockStatus.java
    │   │   └── ReservationStatus.java
    │   ├── controller/StockController.java
    │   ├── dto/StockInfo.java           # 조회 응답 (총 재고·가용·예약 수량)
    │   ├── repository/StockRepository.java
    │   └── service
    │       ├── StockService.java        # 재고 조회
    │       └── StockReservationService.java
    └── product
        ├── Product.java                 # 상품명, 설명, 가격
        ├── controller/ProductController.java
        ├── dto
        │   ├── ProductInfo.java         # 응답용
        │   └── ProductRegisterRequest.java  # 등록/수정 요청용 (초기 재고 수량 포함)
        ├── repository/ProductRepository.java
        └── service/ProductService.java

docs/섹션1                                # 섹션 1 미션 산출물 (설계 문서)
docs/섹션2-데모.http                       # 라이브 데모 시나리오
```

아직 계층형 구조입니다. 섹션 2 안에서 헥사고날(Ports & Adapters)로 재배치할 예정이고, 주문이 상품·재고를 호출하는 지점이 출력 포트가 됩니다.

재고를 `Product` 가 아닌 별도 엔티티로 둔 것은 컨텍스트를 나눈 것이 아닙니다. 재고는 상품 컨텍스트가 소유하되, 쓰기 경합과 변경 주체가 달라 애그리거트만 분리했습니다.

---

## 구현 현황

### 주문 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/order` | 주문 생성 — 상품·재고를 확인하고 재고를 예약 |
| `GET` | `/api/order/{orderId}` | 주문 조회 — 상태와 주문 시점 항목 내역 |
| `PATCH` | `/api/order/{orderId}/cancel` | 주문 취소 — 예약한 재고를 반환 |

### 상품 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/product/{id}` | 상품 단건 조회 |
| `POST` | `/api/product` | 상품 등록 (이름 중복 불가, 초기 재고를 함께 생성) |
| `PUT` | `/api/product/{id}` | 상품 수정 |
| `DELETE` | `/api/product/{id}` | 상품 삭제 |
| `GET` | `/api/product/{productId}/stock` | 재고 조회 — 총 재고·가용·예약 수량과 재고 상태 |
| `GET` | `/api/health` | 헬스 체크 |

### 주문과 재고 예약

주문을 만들면 재고의 **가용 수량이 예약으로 넘어가고**, 결제 결과가 그 예약을 확정하거나 해제합니다. 총 재고가 실제로 줄어드는 시점은 결제가 성공해 예약이 확정될 때입니다.

| 주문 상태 | 언제 | 재고 | 예약 |
|---|---|---|---|
| `PENDING` | 주문 생성 직후 (결제 대기) | 가용 수량 차감 | `RESERVED` |
| `CONFIRMED` | 결제 성공 *(미구현)* | 총 재고 차감 | `CONFIRMED` |
| `CANCELLED` | 사용자 취소 | 가용 수량 복원 | `CANCELLED` |
| `EXPIRED` | 예약 만료 *(배치 미구현)* | 가용 수량 복원 | `EXPIRED` |

취소는 `PENDING` 에서만 가능합니다. 확정된 주문의 취소는 환불이라 결제 취소가 선행되어야 하므로 섹션 3 범위입니다. 사용자 취소와 시간 만료를 상태로 구분해 남기고, 예약 해제는 `RESERVED` 상태에서만 허용해 재고가 중복 복원되지 않게 합니다.

재고 조회의 예약 수량은 따로 저장하지 않고 `총 재고 - 가용 수량` 으로 계산합니다. 예약은 가용 수량만, 확정은 총 재고만 줄이므로 둘의 차이가 곧 확정을 기다리는 수량입니다.

주문 항목(`OrderItem`)은 주문 시점의 상품명과 단가를 복사해 둡니다. 이후 상품 가격이 바뀌어도 이미 끝난 주문의 금액은 달라지지 않고, 조회할 때 상품을 다시 읽지 않습니다. 설계 문서의 **"시점이 중요한 값은 복사한다"** 원칙을 구현한 부분입니다.

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

도메인 코드

| 상황 | 상태 | 코드 |
|---|---|---|
| 없는 상품 조회·수정·삭제 | 404 | `PRODUCT_NOT_FOUND` |
| 이름이 중복된 상품 등록 | 409 | `PRODUCT_ALREADY_EXIST` |
| 없는 주문 조회·취소 | 404 | `ORDER_NOT_FOUND` |
| 없는 상품이 포함된 주문 | 400 | `INVALID_ORDER_ITEM` |
| 이미 취소된 주문을 취소 | 409 | `ORDER_ALREADY_CANCELLED` |
| 만료된 주문을 취소 | 409 | `ORDER_ALREADY_EXPIRED` |
| 확정된 주문을 취소 | 409 | `ORDER_ALREADY_CONFIRMED` |
| 재고 부족 | 409 | `OUT_OF_STOCK` |
| 수량이 1 미만 | 400 | `INVALID_QUANTITY` |
| 재고를 찾을 수 없음 | 404 | `STOCK_NOT_FOUND` |
| 확정할 수 없는 상태의 예약 | 409 | `RESERVATION_NOT_CONFIRMABLE` |
| 해제할 수 없는 상태의 예약 | 409 | `RESERVATION_NOT_CANCELLABLE` |
| 만료된 예약을 확정 | 409 | `RESERVATION_EXPIRED` |

공통 코드

| 상황 | 상태 | 코드 |
|---|---|---|
| 입력값 검증 실패 | 400 | `VALIDATION_FAILED` *(필드별 오류를 `data` 에 담음)* |
| 깨진 JSON, 파라미터 타입 불일치 | 400 | `INVALID_REQUEST` |
| 존재하지 않는 경로 | 404 | `NOT_FOUND` |
| 지원하지 않는 메서드 | 405 | `METHOD_NOT_ALLOWED` |
| 지원하지 않는 Content-Type | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| 그 외 모든 예외 | 500 | `INTERNAL_SERVER_ERROR` |

> 원시 타입(`int`) 필드가 요청 본문에서 빠지면 검증 이전 역직렬화 단계에서 걸려 `VALIDATION_FAILED` 가 아닌 `INVALID_REQUEST` 가 나갑니다. 필드별 메시지가 필요하면 래퍼 타입으로 바꿔야 합니다.

---

## 다음 작업

### 섹션 2 — 헥사고날 전환

- [x] **주문 도메인** — `Order` / `OrderItem` / `OrderStatus`, 주문 생성·조회·취소
      주문 시점의 상품명·단가를 스냅샷으로 복사 (설계 문서의 "시점이 중요한 값은 복사한다" 원칙)
- [x] **재고 예약 연동** — `Stock`(총 재고·가용 수량) / `StockReservation`
      주문 생성이 가용 수량을 예약으로 옮기고, 취소하면 되돌린다
- [ ] **목업 결제** — 성공·실패 두 가지만 던지는 목업 페이지
      성공은 예약 확정, 실패는 재고 복원 (보상 트랜잭션)
- [x] **재고 조회 API** — `GET /api/product/{productId}/stock`
      예약과 복원이 실제로 일어났는지 확인할 수단이 없어 데모에 필요
- [ ] **예약 만료 스케줄러** — 미확정 예약을 배치로 해제
      `expireAt` 과 `isExpired()` 는 있고 배치만 없음
- [ ] **패키지 구조를 헥사고날로 전환** — Ports & Adapters

> 주문을 먼저 만드는 이유: 주문 생성이 상품 컨텍스트를 조회해야 해서 출력 포트가 자연스럽게 필요해집니다.
> 상품 하나만으로는 포트가 리포지토리뿐이라 포트/어댑터 분리의 효과가 드러나지 않습니다.
>
> 결제를 목업으로 두는 이유: 예약을 확정하거나 해제할 주체가 필요한데 실제 PG 연동은 섹션 3 범위입니다.
> 성공·실패만 던지는 목업이면 보상 트랜잭션 흐름은 그대로 성립하고, 나중에 출력 어댑터만 교체하면 됩니다.

### 정리 대상

경계와 관련된 것

- [ ] `Order` ↔ `StockReservation` 양방향 JPA 연관을 `orderId` 참조로 전환
      지금은 주문 컨텍스트와 재고 컨텍스트가 객체 그래프로 묶여 있어 섹션 3 에서 통째로 뜯어야 함
- [ ] 만료 시각의 정본을 하나로 — `Order.expire()` 가 `expireAt` 을 현재 시각으로 덮어써 마감 시각이 사라지고, `StockReservation.expireAt` 과 이중 관리됨
- [ ] `Product` 엔티티의 클래스 레벨 `@Setter` 제거 — 도메인 모델을 분리할 때 가장 먼저 걸리는 지점
- [ ] 도메인별 에러 코드를 각 도메인 패키지로 이동 (`ProductErrorCode` / `OrderErrorCode` / `StockErrorCode` / `ReservationErrorCode`)

동작과 관련된 것

- [ ] 낙관적 락 충돌(`ObjectOptimisticLockingFailureException`)을 409 로 변환 — 현재 500 으로 나감
- [ ] `jakarta.transaction.Transactional` → 스프링의 `@Transactional` (`ProductService`, `readOnly` 사용 불가)
- [ ] `ProductService.findById` 에 읽기 전용 트랜잭션 적용
- [ ] `GlobalExceptionHandler` 슬라이스 테스트 추가 (현재 회귀 방지 없음)
- [ ] `OrderController` 슬라이스 테스트 추가
- [ ] 상품 목록 조회 엔드포인트 *(선택 — 주문 구현에는 불필요)*

### 섹션 3 이후

- 결제 · 정산 컨텍스트 — 목업 결제를 실제 PG 연동으로 교체하고 정산 배치 추가
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

재고 예약 모델(`StockReservation`)은 섹션 2에서 함께 도입합니다. 예약 기록은 보상 트랜잭션을 되돌릴 근거이고, 그 보상을 일으키는 결제는 성공·실패만 던지는 목업으로 대신합니다. PG 연동 없이도 결제 실패 → 재고 복원 흐름이 성립하며, 섹션 3에서는 목업 어댑터만 실제 PG 어댑터로 교체하면 됩니다.

---

## 참고 자료

강의 예제 저장소 [truthplumage/backend_public5](https://github.com/truthplumage/backend_public5)의 학습 문서:

- [DDD 기본 개념](https://github.com/truthplumage/backend_public5/tree/dev/docs/DDD_기본개념) — Bounded Context, Hexagonal Ports & Adapters, Clean Architecture, CQRS, Event-Driven Integration, ACL, Saga & Process Manager
- [전략적 설계](https://github.com/truthplumage/backend_public5/blob/dev/docs/02_전략적_설계.md) / [전술적 설계](https://github.com/truthplumage/backend_public5/blob/dev/docs/03_전술적_설계.md)
- [계층형 아키텍처 비교](https://github.com/truthplumage/backend_public5/blob/dev/docs/04_계층형_아키텍처_비교.md) / [CQRS 및 기타 구현방식](https://github.com/truthplumage/backend_public5/blob/dev/docs/05_CQRS_및_기타_구현방식.md)
- [개발 환경 설정](https://github.com/truthplumage/backend_public5/tree/dev/docs/개발환경) — PostgreSQL, Kafka(Docker), k8s
