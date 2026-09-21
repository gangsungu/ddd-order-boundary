## Domain / Application / Adapter 영역으로 역할 분리

### 영역
|영역|역할|담는 것|
|---|---|---|
|`domain`|업무 규칙|애그리거트, 상태 전이, 에러 코드|
|`application/port/in`|입력 포트|유스케이스 인터페이스와 그 커맨드·조회 결과|
|`application/port/out`|출력 포트|저장소, 다른 컨텍스트 호출. 쓰는 쪽의 언어로 정의한다|
|`application/service`|유스케이스 구현|입력 포트를 구현하고 출력 포트를 쓴다. 트랜잭션 경계|
|`adapter/in`|입력 어댑터|웹 컨트롤러와 요청 DTO(→ 커맨드 변환), 스케줄러|
|`adapter/out`|출력 어댑터|JPA 저장소, 다른 컨텍스트 호출|

+ 의존은 `adapter` → `application` → `domain` 한 방향이다
    - `adapter/in` 은 입력 포트를 부르고, `adapter/out` 은 출력 포트를 구현한다
+ 다른 컨텍스트는 자기 출력 포트로만 부른다
    - 그 포트를 구현하는 `adapter/out` 만 상대 컨텍스트의 입력 포트를 안다

### 컨텍스트별 포트와 어댑터
|컨텍스트|입력 포트|출력 포트|출력 어댑터|
|---|---|---|---|
|주문|`OrderUseCase`|`OrderRepository`, `ProductPort`, `StockPort`|`OrderRepositoryAdapter`, `ProductAdapter`, `StockAdapter`|
|상품 (상품·재고)|`ProductUseCase`, `StockUseCase`|`ProductRepository`, `StockRepository`, `StockReservationRepository`|`ProductRepositoryAdapter`, `StockRepositoryAdapter`, `StockReservationRepositoryAdapter`|
|결제 (목업)|`PaymentUseCase`|`OrderPort`|`OrderAdapter`|

```text
결제                              주문                                         상품 (상품·재고)
PaymentApplicationService
  └─ OrderPort ── OrderAdapter ──→ OrderUseCase
                                   OrderApplicationService
                                     ├─ ProductPort ── ProductAdapter ──→ ProductUseCase
                                     └─ StockPort ──── StockAdapter ────→ StockUseCase
```

+ 의존은 결제 → 주문 → 상품 한 방향이고, 되돌아오는 호출은 없다
+ 출력 포트는 쓰는 쪽의 언어로 정의한다
    - 주문의 `ProductPort` 는 주문 시점 스냅샷에 필요한 ID·이름·단가(`ProductSnapshot`)만 돌려준다. 주문은 상품의 설명이나 상품 쪽 DTO 를 모른다
    - 결제의 `OrderPort` 는 "주문을 확정한다"가 아니라 "결제가 성공했다"(`notifyPaymentSucceeded`)만 말한다. 그 결과로 무엇을 할지는 주문이 정한다
    - 양쪽 표현을 옮기는 번역은 어댑터가 맡는다
+ 재고는 상품 컨텍스트가 소유하므로 `product` 안에 두고, 애그리거트만 `Stock` 으로 나눈다

### 전환 과정
한 번에 옮기지 않고 단계마다 브랜치를 나눴다

|순서|브랜치|내용|
|---|---|---|
|1|`refactor/order-stock-reference`|주문 ↔ 재고 예약의 JPA 연관을 주문 ID 참조로 전환|
|2|`refactor/hexagonal-packages`|모든 컨텍스트를 새 패키지로 이동 (동작·호출 관계 그대로)|
|3|`refactor/hexagonal-product`|상품 컨텍스트 — 저장소 포트와 어댑터, 유스케이스, 커맨드|
|4|`refactor/hexagonal-order`|주문 컨텍스트 — 저장소 포트, 유스케이스, 상품·재고로 나가는 포트와 어댑터|
|5|`refactor/hexagonal-payment`|결제 컨텍스트 — 주문으로 나가는 포트와 어댑터|
|6|`refactor/hexagonal-in-out`|패키지를 in/out 구조로 재배치 (멘토 피드백)|

+ 연관을 먼저 끊었다
    - 끊지 않고 옮기면 상품 컨텍스트로 간 `StockReservation` 이 주문 엔티티를 import 해 경계가 코드에서부터 깨진다
+ 파일 이동과 포트·어댑터 도입을 나눴다
    - 이동만 하는 브랜치는 이름 변경으로 보여 리뷰가 쉽고, 이후 브랜치의 diff 에는 포트·어댑터만 남는다
+ 포트·어댑터는 다른 컨텍스트를 부르는 쪽이 나중에 오도록 의존 방향 순서(상품 → 주문 → 결제)로 들였다
+ 1~5단계는 강의 예제를 따라 `domain / application / infrastructure / presentation` 으로 나눴고, 6단계에서 in/out 으로 옮겼다
    - 패턴은 같고 패키징 관례만 다르다. 방향이 이름에 드러나고, 흩어져 있던 출력 포트(저장소는 `domain/repository`, 다른 컨텍스트는 `application/port`)가 `port/out` 한 곳에 모인다

### 의존 방향 검증
모든 소스의 import 를 영역별로 모아 확인했다

|검사|결과|
|---|---|
|`domain` 이 `application`·`adapter` 를 import|0건|
|포트(`port/in`·`port/out`)가 `service`·`adapter` 를 import|0건|
|`service` 가 `adapter` 를 import|0건|
|포트의 외부 기술 import|0건|
|다른 컨텍스트를 import 하는 파일|출력 어댑터 3개 (`ProductAdapter`, `StockAdapter`, `OrderAdapter`)|

+ 남은 점: `common` 의 웹 코드(`HealthController`, `GlobalExceptionHandler`, `ApiResponse`)가 `adapter` 밖에 있다. 역할로 보면 `adapter/in/web` 이다

domain 이 어떤 외부 기술에 기대고 있는지는 [핵심 도메인이 외부 기술에 직접 의존하지 않도록 구조 개선](핵심-도메인이-외부-기술에-직접-의존하지-않도록-구조-개선.md)에 정리했다
