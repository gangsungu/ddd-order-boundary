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

현재 진행 상황: **섹션 2 진행 중** — 주문 생성·조회·취소, 재고 예약, 목업 결제, 재고 조회와 예약 만료 스케줄러까지 구현했습니다. 지금은 헥사고날 전환을 단계별로 진행하고 있습니다 ([진행 상황](#헥사고날-전환-계획))

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

> 상태 enum 에 값이 추가되면 `./data` 를 지우고 다시 실행해야 합니다. H2 에서는 상태 컬럼이 허용 값을 가진 `ENUM` 타입으로 만들어지고 `ddl-auto: update` 는 기존 컬럼 타입을 바꾸지 않아, 새 값을 저장할 때 500 이 납니다. (예: `OrderStatus.PAYMENT_FAILED` 추가 이전에 만든 DB)

API 를 순서대로 찔러볼 수 있는 시나리오는 [`docs/섹션2-데모.http`](docs/섹션2-데모.http) 에 있습니다. IntelliJ HTTP Client 로 위에서부터 실행하면 상품 등록 → 주문 → 재고 예약 → 결제 확정·실패 → 취소·복원 → 예약 만료 흐름을 그대로 따라갈 수 있습니다. 결제 마감은 기본 10분이라, 예약 만료 구간을 데모에서 바로 보려면 마감과 스케줄러 주기를 줄여서 띄웁니다.

```bash
./gradlew bootRun --args='--order.reservation.expire-time=1 --order.reservation.expire-check-interval=5'
```

결제는 브라우저에서도 할 수 있습니다. <http://localhost:8080/payment.html?orderId=1> 을 열면 주문 내역과 상품별 재고(총 재고·가용·예약)가 보이고, **결제 성공** / **결제 실패** 버튼으로 결과를 보낼 수 있습니다.

---

## 프로젝트 구조

```
src/main/java/com/roykhan/dddorderboundary
├── DddOrderBoundaryApplication.java
├── common                               # 컨텍스트가 함께 쓰는 기반
│   ├── config
│   │   ├── JacksonConfig.java           # ApiResponse 전용 직렬화 모듈 등록
│   │   ├── JpaAuditingConfig.java
│   │   └── SchedulingConfig.java        # @EnableScheduling (슬라이스 테스트에서 빠지도록 분리)
│   ├── controller
│   │   └── HealthController.java
│   ├── domain
│   │   └── BaseEntity.java              # 공통 식별자 + 생성/수정 시각 감사(Auditing)
│   ├── exception
│   │   ├── BaseErrorCode.java           # 에러 코드 계약 (상태/메시지/예외 생성)
│   │   ├── BusinessException.java       # 에러 코드를 실어 나르는 도메인 예외
│   │   ├── CommonErrorCode.java         # 공통 4xx/5xx 코드
│   │   └── GlobalExceptionHandler.java  # 예외 → ApiResponse 변환
│   └── response
│       ├── ApiResponse.java             # 모든 응답의 공통 규격
│       └── ApiResponseSerializer.java   # 성공 시 data 생략, 실패 시 유지
├── order                                # 주문 컨텍스트
│   ├── domain
│   │   ├── model
│   │   │   ├── Order.java               # 주문 애그리거트 루트 — 상태 전이와 총액 계산
│   │   │   ├── OrderItem.java           # 주문 시점 상품명·단가 스냅샷
│   │   │   └── OrderStatus.java
│   │   ├── repository/OrderRepository.java  # 출력 포트 — 일반 인터페이스
│   │   └── exception/OrderErrorCode.java
│   ├── application
│   │   ├── usecase/OrderUseCase.java    # 입력 포트 — 컨트롤러·스케줄러·결제가 이것으로 들어온다
│   │   ├── service/OrderApplicationService.java
│   │   ├── port                         # 다른 컨텍스트로 나가는 출력 포트 (주문 쪽 언어)
│   │   │   ├── ProductPort.java / ProductSnapshot.java  # 주문 시점 스냅샷용 이름·단가
│   │   │   └── StockPort.java / StockLine.java          # 주문 ID 단위 예약·확정·해제·만료
│   │   └── dto
│   │       ├── CreateOrderCommand.java
│   │       └── OrderInfo.java           # 조회 결과 (항목 내역 포함)
│   ├── infrastructure
│   │   ├── persistence                  # 출력 어댑터 — 주문 저장소 포트를 Spring Data JPA 로 구현
│   │   │   └── OrderRepositoryAdapter.java / OrderJpaRepository.java
│   │   └── product                      # 출력 어댑터 — 상품·재고 포트를 상품 컨텍스트의 유스케이스로 구현
│   │       ├── ProductAdapter.java
│   │       └── StockAdapter.java
│   └── presentation
│       ├── controller/OrderController.java
│       ├── scheduler/OrderExpirationScheduler.java  # 결제 마감이 지난 주문 만료
│       └── dto
│           ├── CreateOrderRequest.java  # 생성 요청 (중첩 OrderLine) → 커맨드로 변환
│           └── OrderCreateInfo.java     # 생성 응답 (주문 ID)
├── product                              # 상품 컨텍스트 — 상품과 재고
│   ├── domain
│   │   ├── model
│   │   │   ├── Product.java             # 상품명, 설명, 가격
│   │   │   ├── Stock.java               # 총 재고와 가용 수량, 낙관적 락
│   │   │   ├── StockReservation.java    # 예약 주체(주문 ID)·수량·만료 시각
│   │   │   ├── StockStatus.java
│   │   │   └── ReservationStatus.java
│   │   ├── repository                   # 출력 포트 — 일반 인터페이스
│   │   │   ├── ProductRepository.java
│   │   │   ├── StockRepository.java
│   │   │   └── StockReservationRepository.java  # 주문 ID 로 예약 조회
│   │   └── exception
│   │       ├── ProductErrorCode.java
│   │       ├── StockErrorCode.java
│   │       └── ReservationErrorCode.java
│   ├── application
│   │   ├── usecase                      # 입력 포트 — 컨텍스트 바깥은 이것으로만 들어온다
│   │   │   ├── ProductUseCase.java
│   │   │   └── StockUseCase.java        # 재고 조회, 주문 ID 단위 예약·확정·해제·만료
│   │   ├── service
│   │   │   ├── ProductApplicationService.java
│   │   │   └── StockApplicationService.java
│   │   └── dto
│   │       ├── RegisterProductCommand.java
│   │       ├── UpdateProductCommand.java    # 재고 수량은 담지 않음
│   │       ├── ReserveStockCommand.java     # 한 주문의 항목들을 같은 결제 마감으로 예약
│   │       ├── ProductInfo.java         # 상품 조회 결과
│   │       └── StockInfo.java           # 재고 조회 결과 (총 재고·가용·예약 수량)
│   ├── infrastructure
│   │   └── persistence                  # 출력 어댑터 — 저장소 포트를 Spring Data JPA 로 구현
│   │       ├── ProductRepositoryAdapter.java / ProductJpaRepository.java
│   │       ├── StockRepositoryAdapter.java / StockJpaRepository.java
│   │       └── StockReservationRepositoryAdapter.java / StockReservationJpaRepository.java
│   └── presentation
│       ├── controller
│       │   ├── ProductController.java
│       │   └── StockController.java
│       └── dto/ProductRegisterRequest.java  # 등록/수정 요청 → 커맨드로 변환
└── payment                              # 결제 컨텍스트 (목업) — 상태 없이 결과만 주문에 전달
    ├── domain/model/PaymentResult.java  # SUCCESS / FAILURE
    ├── application/service/PaymentService.java  # 결과를 주문 확정·결제 실패 호출로 변환
    └── presentation
        ├── controller/PaymentController.java
        └── dto/PaymentResultRequest.java

src/main/resources/static/payment.html    # 목업 결제 페이지
docs/섹션1                                # 섹션 1 미션 산출물 (설계 문서)
docs/섹션2-데모.http                       # 라이브 데모 시나리오
```

컨텍스트마다 `domain / application / infrastructure / presentation` 으로 나눕니다. 지금은 상품·주문 컨텍스트까지 포트·어댑터를 들였고, 결제는 아직 패키지만 나눈 상태라 `infrastructure` 가 없습니다. 목표 구조와 순서, 지금 남아 있는 의존은 [헥사고날 전환 계획](#헥사고날-전환-계획)에 정리했습니다.

재고를 `Product` 가 아닌 별도 엔티티로 둔 것은 컨텍스트를 나눈 것이 아닙니다. 재고는 상품 컨텍스트가 소유하되, 쓰기 경합과 변경 주체가 달라 애그리거트만 분리했습니다. 그래서 패키지도 따로 두지 않고 `product` 컨텍스트 안에 함께 둡니다.

---

## 구현 현황

### 주문 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/order` | 주문 생성 — 상품·재고를 확인하고 재고를 예약 |
| `GET` | `/api/order/{orderId}` | 주문 조회 — 상태와 주문 시점 항목 내역 |
| `PATCH` | `/api/order/{orderId}/cancel` | 주문 취소 — 예약한 재고를 반환 |

### 결제 API (목업)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/payment/{orderId}/result` | 결제 결과 반영 — 본문 `{"result": "SUCCESS" \| "FAILURE"}` |
| `GET` | `/payment.html?orderId={orderId}` | 결과를 버튼으로 보내는 목업 결제 페이지 |

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
| `CONFIRMED` | 결제 성공 | 총 재고 차감 | `CONFIRMED` |
| `PAYMENT_FAILED` | 결제 실패 | 가용 수량 복원 | `CANCELLED` |
| `CANCELLED` | 사용자 취소 | 가용 수량 복원 | `CANCELLED` |
| `EXPIRED` | 결제 마감 경과 (스케줄러) | 가용 수량 복원 | `EXPIRED` |

취소와 결제 결과 반영은 `PENDING` 에서만 가능합니다. 확정된 주문의 취소는 환불이라 결제 취소가 선행되어야 하므로 섹션 3 범위입니다. 사용자 취소·결제 실패·시간 만료를 주문 상태로 구분해 남기고, 예약 해제는 `RESERVED` 상태에서만 허용해 재고가 중복 복원되지 않게 합니다. 결제 실패로 인한 해제는 재고 입장에서 사용자 취소와 다르지 않아 예약은 `CANCELLED` 로 남기고, 실패 사유는 주문 상태가 가집니다.

주문과 재고 예약은 객체로 묶지 않고 **주문 ID 로만 연결**합니다. `Order` 는 예약을 들지 않고 자기 상태 전이만 책임지며, 예약(`StockReservation`)이 `orderId` 를 가집니다. 주문 서비스는 주문 상태를 먼저 바꾼 뒤, 같은 트랜잭션 안에서 그 주문 ID 의 예약을 확정·해제·만료하도록 재고 쪽에 요청합니다.

```text
OrderApplicationService.confirmOrder(orderId)
 ├─ Order.confirm()           주문 상태 전이 (PENDING → CONFIRMED), 불가능하면 여기서 예외
 └─ StockPort.confirm(orderId)
     └─ StockAdapter → StockUseCase.confirmReservations(orderId)   그 주문의 예약을 모두 확정 → 총 재고 차감
```

- 주문 상태 검사에 걸리면 재고 쪽은 호출하지 않습니다. 예약 쪽에서 예외가 나면(`RESERVATION_EXPIRED` 등) 트랜잭션이 함께 되돌아가 주문 상태도 원래대로 남습니다.
- 예약 주체는 다른 컨텍스트의 주문이라 객체 그래프로 묶어 두면 컨텍스트를 나눌 때 통째로 뜯어야 합니다. ID 로만 참조하면 지금의 서비스 호출이 헥사고날 전환에서 그대로 재고 출력 포트가 되고, 섹션 3 에서 네트워크 호출로 바꿔도 주문 도메인은 바뀌지 않습니다.
- 재고 레코드가 있는지는 주문이 아니라 재고 쪽이 예약하면서 확인합니다. 상품은 있는데 재고가 없으면 `404 STOCK_NOT_FOUND` 가 나갑니다.

재고 조회의 예약 수량은 따로 저장하지 않고 `총 재고 - 가용 수량` 으로 계산합니다. 예약은 가용 수량만, 확정은 총 재고만 줄이므로 둘의 차이가 곧 확정을 기다리는 수량입니다.

결제 마감(`expireAt`)이 지난 예약은 결제에 성공해도 `RESERVATION_EXPIRED` 로 거절합니다. 반대로 결제 실패는 마감 이후에도 받아 재고를 돌려줍니다. 마감이 지나고 스케줄러가 돌기 전까지의 짧은 구간에서 두 경우가 갈립니다.

만료 스케줄러(`OrderExpirationScheduler`)는 `order.reservation.expire-check-interval`(기본 30초)마다 결제 마감이 지난 `PENDING` 주문을 마감이 이른 순으로 최대 100건씩 만료시킵니다.

- 주문마다 트랜잭션을 따로 잡아 한 건이 실패해도 나머지 만료는 되돌아가지 않습니다.
- ID 를 읽은 뒤 결제·취소가 먼저 끝난 주문은 `Order.expire()` 의 상태 검사에서 걸러져 건너뜁니다.
- 결제·취소와 같은 재고를 동시에 바꾸면 `Stock` 의 낙관적 락이 한쪽을 되돌립니다. 만료가 밀려났다면 주문이 아직 `PENDING` 이라 다음 주기에 다시 잡힙니다.
- 만료해도 결제 마감 시각은 그대로 두고, 풀린 시각은 `cancelledAt` 에 남깁니다.

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
| 없는 주문 조회·취소·결제 | 404 | `ORDER_NOT_FOUND` |
| 없는 상품이 포함된 주문 | 400 | `INVALID_ORDER_ITEM` |
| 이미 취소된 주문을 취소·결제 | 409 | `ORDER_ALREADY_CANCELLED` |
| 만료된 주문을 취소·결제 | 409 | `ORDER_ALREADY_EXPIRED` |
| 확정된 주문을 취소·결제 | 409 | `ORDER_ALREADY_CONFIRMED` |
| 결제에 실패한 주문을 취소·결제 | 409 | `ORDER_PAYMENT_FAILED` |
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
- [x] **목업 결제** — 성공·실패 두 가지만 던지는 목업 페이지
      성공은 예약 확정, 실패는 재고 복원 (보상 트랜잭션)
- [x] **재고 조회 API** — `GET /api/product/{productId}/stock`
      예약과 복원이 실제로 일어났는지 확인할 수단이 없어 데모에 필요
- [x] **예약 만료 스케줄러** — 미확정 예약을 배치로 해제
      결제 마감이 지난 `PENDING` 주문을 주기적으로 만료시켜 재고를 돌려준다
- [ ] **헥사고날(Ports & Adapters) 전환** — 단계마다 브랜치를 나눠 진행 ([계획](#헥사고날-전환-계획))

> 주문을 먼저 만드는 이유: 주문 생성이 상품 컨텍스트를 조회해야 해서 출력 포트가 자연스럽게 필요해집니다.
> 상품 하나만으로는 포트가 리포지토리뿐이라 포트/어댑터 분리의 효과가 드러나지 않습니다.
>
> 결제를 목업으로 두는 이유: 예약을 확정하거나 해제할 주체가 필요한데 실제 PG 연동은 섹션 3 범위입니다.
> 성공·실패만 던지는 목업이면 보상 트랜잭션 흐름은 그대로 성립하고, 나중에 출력 어댑터만 교체하면 됩니다.

### 헥사고날 전환 계획

한 번에 옮기지 않고 단계마다 브랜치를 나눕니다. 패키지 이동과 포트·어댑터 도입을 분리하고, 포트·어댑터는 다른 컨텍스트를 부르는 쪽이 나중에 오도록 의존 방향 순서로 들입니다.

| 순서 | 브랜치 | 내용 | 상태 |
|---|---|---|---|
| 1 | `refactor/order-stock-reference` | 주문 ↔ 재고 예약의 JPA 연관을 `orderId` 참조로 전환 | 완료 |
| 2 | `refactor/hexagonal-packages` | 모든 컨텍스트를 새 패키지 구조로 이동 — 동작과 호출 관계는 그대로, `BaseEntity`·에러 코드도 제자리로 | 완료 |
| 3 | `refactor/hexagonal-product` | 상품 컨텍스트(상품·재고) — 저장소 포트와 어댑터, 상품·재고 유스케이스 | 완료 |
| 4 | `refactor/hexagonal-order` | 주문 컨텍스트 — 상품·재고를 부르는 출력 포트와 어댑터 | 완료 |
| 5 | `refactor/hexagonal-payment` | 결제 컨텍스트 — 주문을 부르는 출력 포트와 어댑터 | |

- 연관을 먼저 끊는 이유: 끊지 않고 옮기면 상품 컨텍스트로 간 `StockReservation` 이 주문 엔티티를 import 해, 컨텍스트 경계가 코드에서부터 깨집니다.
- 패키지 이동을 따로 떼는 이유: 이동만 하는 브랜치는 git 이 이름 변경으로 보여 줘 리뷰가 쉽고, 이후 브랜치의 diff 에는 포트·어댑터만 남습니다. 옛 구조와 새 구조가 섞이는 기간도 생기지 않습니다. 대신 2단계 직후에는 폴더만 헥사고날 모양이고, 서비스가 다른 컨텍스트를 직접 부르는 상태가 3~5단계까지 남습니다.

목표 구조는 강의 예제 저장소와 같은 이름을 씁니다. 컨텍스트마다 네 계층을 두고, 도메인 모델은 JPA 엔티티를 겸합니다.

| 계층 | 역할 | 예 (주문 컨텍스트) |
|---|---|---|
| `domain` | 모델과 규칙, 저장소 포트 | `model/Order`, `repository/OrderRepository` |
| `application` | 유스케이스(입력 포트)와 구현, 다른 컨텍스트로 나가는 출력 포트 | `usecase/OrderUseCase`, `service/OrderApplicationService`, `port/StockPort` |
| `infrastructure` | 출력 포트 구현 — JPA, 다른 컨텍스트 호출 | `persistence/OrderRepositoryAdapter`, `product/StockAdapter` |
| `presentation` | 입력 어댑터 — 컨트롤러, 스케줄러 | `controller/OrderController`, `scheduler/OrderExpirationScheduler` |

- 의존은 `presentation`·`infrastructure` → `application` → `domain` 한 방향으로만 향합니다.
- 다른 컨텍스트는 그 컨텍스트의 유스케이스로만 부르고, 부르는 쪽은 자기 출력 포트를 거칩니다. 섹션 3 에서 호출 방식이 HTTP·메시지로 바뀌어도 어댑터만 교체하면 됩니다.
- 도메인 모델과 JPA 엔티티는 분리하지 않습니다. 섹션 2 의 목적은 포트·어댑터로 경계를 드러내는 것이고, 분리가 필요해지면 어댑터 안쪽만 바뀝니다.

#### 컨텍스트별 포트와 어댑터

**상품 컨텍스트** (3단계)

```text
ProductController / StockController                  presentation          입력 어댑터
      │  ProductUseCase / StockUseCase                application/usecase   입력 포트
      ▼
ProductApplicationService / StockApplicationService  application/service
      │  ProductRepository / StockRepository
      │  / StockReservationRepository                 domain/repository     출력 포트
      ▼
*RepositoryAdapter → *JpaRepository                  infrastructure/persistence  출력 어댑터
```

| 구분 | 이름 | 설명 |
|---|---|---|
| 입력 포트 | `ProductUseCase` | 조회·여러 건 조회·등록·수정·삭제. 주문도 상품 정보를 이것으로 읽는다 |
| 입력 포트 | `StockUseCase` | 재고 조회와 주문 ID 단위 예약·확정·해제·만료. 재고 조회 서비스와 예약 서비스를 하나로 합쳤다 |
| 출력 포트 | `ProductRepository` · `StockRepository` · `StockReservationRepository` | Spring Data 를 모르는 일반 인터페이스 |
| 출력 어댑터 | `ProductRepositoryAdapter` · `StockRepositoryAdapter` · `StockReservationRepositoryAdapter` | 같은 이름의 `*JpaRepository`(Spring Data)에 위임 |

- 서비스는 요청 DTO 대신 커맨드(`RegisterProductCommand` · `UpdateProductCommand` · `ReserveStockCommand`)를 받습니다. 요청 DTO 를 커맨드로 바꾸는 일은 presentation 이 맡아, application → presentation 역방향 의존이 사라졌습니다.
- 수정 커맨드에는 재고 수량이 없습니다. 수정 요청은 등록과 같은 DTO 를 써서 `initialQuantity` 를 받지만, 커맨드로 바꿀 때 버립니다.
- `Product` 는 `@Setter` 대신 `update()` 로만 바뀝니다.
- 상품 컨텍스트 바깥(주문)은 상품의 저장소를 만지지 않고 `ProductUseCase` · `StockUseCase` 로만 들어옵니다.

**주문 컨텍스트** (4단계)

```text
OrderController / OrderExpirationScheduler / (결제)  presentation          입력 어댑터
      │  OrderUseCase                                application/usecase   입력 포트
      ▼
OrderApplicationService                              application/service
      ├─ OrderRepository                             domain/repository     출력 포트 ─→ OrderRepositoryAdapter → OrderJpaRepository
      ├─ ProductPort                                 application/port      출력 포트 ─→ ProductAdapter → ProductUseCase (상품 컨텍스트)
      └─ StockPort                                   application/port      출력 포트 ─→ StockAdapter   → StockUseCase   (상품 컨텍스트)
```

| 구분 | 이름 | 설명 |
|---|---|---|
| 입력 포트 | `OrderUseCase` | 생성·조회·취소·결제 확정·결제 실패·만료 대상 조회·만료 |
| 출력 포트 | `OrderRepository` | 주문 저장소. 만료 대상 조회는 페이지 대신 건수(`limit`)만 받는다 |
| 출력 포트 | `ProductPort` | 주문 시점 스냅샷에 필요한 상품 ID·이름·단가(`ProductSnapshot`)만 읽는다 |
| 출력 포트 | `StockPort` | 주문 ID 로 예약(`StockLine` 목록)·확정·해제·만료를 요청한다 |
| 출력 어댑터 | `OrderRepositoryAdapter` | 건수를 Spring Data 의 첫 페이지 요청으로 바꿔 `OrderJpaRepository` 에 위임 |
| 출력 어댑터 | `ProductAdapter` · `StockAdapter` | 상품 컨텍스트의 유스케이스를 부르고, 상품 쪽 표현(`ProductInfo` · `ReserveStockCommand`)과 주문 쪽 표현을 서로 옮긴다 |

- 출력 포트는 주문 쪽 언어로 정의합니다. 주문은 상품의 설명이나 재고 예약 커맨드의 모양을 모르고, 필요한 것만 자기 타입으로 받습니다. 주문의 `domain` · `application` · `presentation` 에는 상품 컨텍스트 import 가 하나도 없고, 상품을 아는 곳은 `infrastructure/product` 의 어댑터 둘뿐입니다.
- 다른 컨텍스트로 나가는 포트는 `domain/repository` 가 아니라 `application/port` 에 둡니다. 저장소는 주문 애그리거트를 저장하는 도메인의 필요이고, 상품·재고 호출은 유스케이스를 수행하려는 애플리케이션의 필요이기 때문입니다.
- 어댑터는 같은 JVM 안에서 상품 컨텍스트의 유스케이스를 부르므로, 재고 변경이 주문 트랜잭션에 함께 묶입니다. 재고보다 많이 주문하면 주문 저장까지 함께 되돌아갑니다. 섹션 3 에서 네트워크 호출로 바꾸면 이 보장이 사라지므로 어댑터를 교체하면서 보상 흐름(Saga)을 따로 세워야 합니다.
- 서비스는 요청 DTO 대신 `CreateOrderCommand` 를 받습니다. 결제 컨텍스트도 이제 `OrderService` 가 아닌 `OrderUseCase` 로 주문을 부릅니다.

#### 지금 남은 의존

| 위치 | 지금 남은 의존 | 없애는 단계 |
|---|---|---|
| `PaymentService` | 주문 컨텍스트의 `OrderUseCase` 를 직접 호출 | 5 결제 — 주문 출력 포트와 어댑터 |
| 재고 포트의 예외 | 상품 컨텍스트의 에러 코드(`OUT_OF_STOCK` · `RESERVATION_EXPIRED` 등)가 번역 없이 주문 API 응답까지 그대로 나간다 | 섹션 3 — ACL 에서 주문 쪽 의미로 번역 |

- 저장소 인터페이스는 2단계부터 포트 자리(`domain/repository`)에 두었습니다. 상품·주문 컨텍스트에서 포트와 어댑터로 쪼갤 때 서비스 코드는 저장소 쪽으로 바뀌지 않았습니다.
- 서비스 이름은 유스케이스 인터페이스(입력 포트)를 들이는 단계에서 `*ApplicationService` 로 바꿉니다. (상품 3단계, 주문 4단계)
- enum 은 따로 `enums` 패키지를 두지 않고 모델과 함께 `domain/model` 에 둡니다.

### 정리 대상

경계와 관련된 것

- [x] `Order` ↔ `StockReservation` 양방향 JPA 연관을 `orderId` 참조로 전환
      주문은 예약을 들지 않고, 예약은 주문 ID 만 가진다. 확정·해제는 주문 서비스가 주문 ID 로 요청한다
- [ ] 만료 시각의 정본을 하나로 — `Order.expireAt` 과 `StockReservation.expireAt` 이 이중 관리됨
      스케줄러는 주문의 마감을, 결제 확정은 예약의 마감을 본다. 지금은 생성 시 같은 값을 넣어 어긋나지 않을 뿐이다
- [x] `Product` 엔티티의 클래스 레벨 `@Setter` 제거 — 도메인 모델을 분리할 때 가장 먼저 걸리는 지점
      수정은 `Product.update()` 로만 한다. 기본 생성자도 JPA 용으로 `protected` 로 좁혔다
- [x] 도메인별 에러 코드를 각 도메인 패키지로 이동 (`ProductErrorCode` / `OrderErrorCode` / `StockErrorCode` / `ReservationErrorCode`)
      `order/domain/exception`, `product/domain/exception` 으로 옮겼다. 공통 계약(`BaseErrorCode`)과 공통 코드만 `common/exception` 에 남는다

동작과 관련된 것

- [ ] 낙관적 락 충돌(`ObjectOptimisticLockingFailureException`)을 409 로 변환 — 현재 500 으로 나감
- [x] `jakarta.transaction.Transactional` → 스프링의 `@Transactional` (`ProductService`, `readOnly` 사용 불가)
      스프링 것으로 바꿔 조회에 `readOnly` 를 쓸 수 있게 했다
- [x] `ProductService.findById` 에 읽기 전용 트랜잭션 적용
      상품 조회 메서드에 `readOnly = true`
- [ ] 상품을 삭제해도 재고가 남음 — `Stock` 은 `productId` 로만 상품을 가리켜, 상품을 지워도 재고 행은 그대로다
- [ ] 상품 수정 요청을 등록 요청과 분리 — 지금은 같은 DTO 를 써서 수정에 쓰지 않는 `initialQuantity` 까지 필수로 받는다
- [ ] `GlobalExceptionHandler` 슬라이스 테스트 추가 (현재 회귀 방지 없음)
- [x] `OrderController` 슬라이스 테스트 추가
      요청 → 커맨드 변환, 검증 실패, 조회 응답, 취소와 409 를 확인한다
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
