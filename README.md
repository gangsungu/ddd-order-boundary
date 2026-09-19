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

현재 진행 상황: **섹션 1 — 설계 문서 작성 + 모놀리식 CRUD 뼈대 구현**

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
└── domain
    ├── base
    │   └── BaseEntity.java          # 공통 식별자 + 생성/수정 시각 감사(Auditing)
    ├── order
    │   └── Order.java
    └── product
        ├── Product.java             # 상품명, 설명, 가격, 재고 상태
        ├── StockStatus.java         # SOLD_OUT / PROCESSING / IN_STOCK
        ├── controller/ProductController.java
        ├── repository/ProductRepository.java
        └── service/ProductService.java

docs/섹션1                            # 섹션 1 미션 산출물 (설계 문서)
```

섹션 2부터는 이 패키지 구조를 헥사고날(`presentation` / `application` / `domain` / `infrastructure`) 구조로 리팩터링할 예정입니다.

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
