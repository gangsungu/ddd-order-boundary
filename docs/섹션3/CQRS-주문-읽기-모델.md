## CQRS — 주문 읽기 모델

### 왜 필요했나
+ 조회가 쓰기 모델(`Order`)을 읽어 응답을 만들었다
    - 조회 응답의 모양이 바뀌면 쓰기 모델이나 쓰기 서비스가 함께 바뀐다
    - 조회를 위해 애그리거트와 항목 연관을 매번 불러온다
+ 포트 이름만 `Command` · `Query` 로 나누면 같은 테이블을 읽는 한 구조에서 구분되지 않는다
    - 그래서 **저장소까지** 나눴다. 조회는 이벤트로 갱신되는 별도 테이블만 읽는다

### 구조
```text
쓰기 (Command)                                           읽기 (Query)
OrderController ─ POST·PATCH                             OrderController ─ GET
  └─ OrderCommandUseCase                                   └─ OrderQueryUseCase
      OrderCommandService                                      OrderQueryService
        ├─ Order (orders.orders)                                 └─ OrderViewRepository ── orders.order_view
        └─ OrderEventPublisher ── OrderPlaced / OrderConfirmed / …           ▲
                                   │ 커밋 뒤                                  │
                                   ▼                                         │
                              OrderEventListener ── OrderProjectionUseCase ──┘
                                                     OrderProjectionService
```

|구분|쓰기|읽기|
|---|---|---|
|입력 포트|`OrderCommandUseCase`|`OrderQueryUseCase`, `OrderProjectionUseCase`|
|서비스|`OrderCommandService`|`OrderQueryService`, `OrderProjectionService`|
|모델|`Order` · `OrderItem` (애그리거트, JPA 엔티티)|`OrderInfo` (조회 응답 모양의 record, JPA 를 모름)|
|저장소|`OrderRepository` → `orders.orders` · `orders.order_items`|`OrderViewRepository` → `orders.order_view` · `orders.order_view_items`|

### 이벤트 → 읽기 모델
|이벤트|읽기 모델|
|---|---|
|`OrderPlaced`|`PENDING` 으로 새로 만든다. 항목 금액은 여기서 계산해 저장한다|
|`OrderConfirmed`|`CONFIRMED`, `confirmedAt`|
|`OrderCancelled` · `OrderPaymentFailed` · `OrderExpired`|각 상태, `cancelledAt`|

+ `OrderProjectionService` 는 이벤트만 본다. 쓰기 모델을 다시 읽지 않는다
+ 상태 검사는 쓰기 쪽이 끝냈으므로 읽기 쪽은 이벤트를 그대로 옮긴다

### 결정
+ **같은 스키마(`orders`), 다른 테이블**
    - 읽기 모델도 주문 컨텍스트가 소유한다. 컨텍스트별 스키마 원칙을 지키면서 쓰기 모델과는 테이블로 나눴다
+ **`OrderInfo` 를 읽기 모델로 삼았다**
    - 조회 응답의 모양 그대로 저장하고 그대로 돌려준다. 조회 API 의 응답 형식은 바뀌지 않았다
    - JPA 매핑은 `adapter/out/persistence/view/OrderViewEntity` 에만 둔다. 쓰기 모델은 JPA 애너테이션을 도메인에 두는 트레이드오프를 택했지만([섹션 2](../섹션2/핵심-도메인이-외부-기술에-직접-의존하지-않도록-구조-개선.md)), 규칙이 없는 읽기 모델은 처음부터 분리했다
+ **커밋 뒤에 갱신한다** (`AFTER_COMMIT`)
    - 롤백된 주문은 읽기 모델에 나타나지 않는다
    - 대가로 쓰기와 읽기 사이에 틈이 생긴다(결과적 일관성)
+ **만료 대상 조회는 쓰기 쪽에 남겼다**
    - `findExpiredOrderIds` 는 화면에 보여 줄 조회가 아니라 만료라는 쓰기를 하려고 쓰기 모델을 읽는 것이다

### 한계
+ 지금은 같은 스레드에서 이어져 API 가 응답할 때 이미 반영되어 있다. Kafka 를 건너면 주문 직후 조회가 `ORDER_NOT_FOUND` 일 수 있다
+ 갱신이 실패하면 읽기 모델만 뒤처지고 로그가 남는다. 쓰기 모델에서 읽기 모델을 다시 만드는 재구축은 아직 없다
+ 읽기 모델이 하나뿐이라, 조회 요구가 늘면(회원별 주문 목록 등) 그 모양의 읽기 모델을 이벤트로 하나씩 더 만든다

### 테스트
|테스트|확인하는 것|
|---|---|
|`OrderProjectionServiceTest`|이벤트마다 읽기 모델을 어떻게 바꾸는지, 읽기 모델에 없는 주문의 이벤트를 건너뛰는지|
|`OrderQueryServiceTest`|조회가 읽기 모델만 보는지|
|`OrderEventListenerTest`|갱신 실패를 쓰기 쪽으로 던지지 않는지|
|`OrderReadModelFlowTest`|생성·취소가 읽기 모델까지 이어지는지, 롤백된 주문은 나타나지 않는지, 읽기 모델을 지우면 주문이 있어도 조회되지 않는지|
