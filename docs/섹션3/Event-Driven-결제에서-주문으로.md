## Event-Driven — 결제에서 주문으로, 주문에서 읽기 모델로

### 왜 필요했나
```text
전                                                후
PaymentApplicationService                         PaymentApplicationService
  └─ OrderPort ── OrderAdapter ──→ OrderUseCase     └─ PaymentEventPublisher ── PaymentSucceeded / PaymentFailed
     (결제가 주문을 안다)                                                            │ 커밋 뒤
                                                                                    ▼
                                                                          주문 PaymentEventListener
```

+ 결제가 주문의 유스케이스를 알고 있었다
    - 결제 뒤에 할 일이 늘어나면(정산, 알림) 결제가 그 컨텍스트들을 모두 불러야 한다
    - 주문의 실패가 결제의 실패처럼 결제 API 응답에 섞였다
+ 이제 결제는 받는 쪽을 모른다. 받는 쪽이 늘어도 결제는 바뀌지 않는다

### 이벤트
|발행|이벤트|받는 곳|
|---|---|---|
|결제|`PaymentSucceeded` · `PaymentFailed`|주문 `PaymentEventListener` → 확정·결제 실패|
|주문|`OrderPlaced` · `OrderConfirmed` · `OrderCancelled` · `OrderPaymentFailed` · `OrderExpired`|주문 `OrderEventListener` → 읽기 모델 갱신 (CQRS)|

+ 이벤트는 발행하는 컨텍스트의 `domain/event` 에 두고, `sealed` 인터페이스로 종류를 닫았다
+ 주문 이벤트는 상태가 바뀔 때마다 하나씩 나간다. 받는 쪽이 이벤트만 보고 상태를 따라갈 수 있어야 하기 때문이다

### 발행과 전달
|자리|역할|
|---|---|
|`application/port/out/*EventPublisher`|서비스가 쓰는 출력 포트. 누가 받는지 모른다|
|`adapter/out/event/Spring*EventPublisher`|스프링 `ApplicationEventPublisher` 로 발행한다. 섹션 4 에서 Kafka 발행 어댑터로 바꾸는 자리|
|`adapter/in/.../*EventListener`|`@TransactionalEventListener(AFTER_COMMIT)` 로 받는 입력 어댑터|

+ **커밋된 사실만 알린다** (`AFTER_COMMIT`)
    - 재고가 부족해 되돌아간 주문의 `OrderPlaced` 는 받는 쪽에 닿지 않는다
    - 결제 서비스에 `@Transactional` 을 건 것도 이 때문이다. 아직 결제가 저장하는 상태는 없지만, 결제 기록이 생기면 "기록이 커밋된 결제만 알린다"가 된다
+ **리스너는 트랜잭션을 떼어 낸다** (`@Transactional(propagation = NOT_SUPPORTED)`)
    - 커밋 직후에는 끝난 트랜잭션이 아직 스레드에 묶여 있다. 받는 쪽 서비스가 그대로 합류하면 변경이 커밋되지 않는다
    - 떼어 두면 받는 쪽 서비스가 자기 트랜잭션을 새로 연다
    - 이 설정을 빼면 `PaymentEventFlowTest` · `OrderReadModelFlowTest` 가 실제로 실패한다
+ **받는 쪽의 실패는 보내는 쪽으로 돌아가지 않는다**
    - 결제 이벤트: 주문의 거절은 로그로 남기고 끝낸다
    - 주문 이벤트: 읽기 모델 갱신이 실패해도 쓰기는 그대로다

### 결정
+ **전달은 스프링 인프로세스 이벤트로 했다**
    - 섹션 3 은 패턴의 구조를 세우는 단계이고, 메시지 브로커는 섹션 4(Kafka) 범위다
    - 포트 · 발행 어댑터 · 리스너를 나눠 두어, 섹션 4 에서는 발행 어댑터와 리스너의 입구만 바뀐다
+ **이벤트에는 받는 쪽이 필요한 것을 싣는다**
    - `OrderPlaced` 에 항목까지 실은 것은 읽기 모델이 쓰기 모델을 다시 읽지 않게 하기 위해서다
    - 발생 시각은 애그리거트가 상태를 바꾸며 남긴 시각(`confirmedAt` 등)을 그대로 쓴다

### 한계
+ 같은 스레드의 동기 전달이라 지금은 사실상 즉시 반영된다. 비동기(Kafka)가 되면 아래가 새로 필요하다
    - 발행 보장: 커밋과 발행 사이에 죽으면 이벤트가 사라진다 → Outbox
    - 중복·순서: 같은 이벤트를 두 번 받거나 순서가 바뀔 수 있다 → 멱등 처리
+ 결제 성공인데 주문이 받지 못한 경우(이미 만료) 결제는 성공으로 남는다. 환불로 보상하는 흐름은 Saga 에서 세운다

### 테스트
|테스트|확인하는 것|
|---|---|
|`PaymentApplicationServiceTest`|결제 결과를 어떤 이벤트로 발행하는지|
|`OrderCommandServiceTest`|상태 전이마다 맞는 이벤트를 발행하는지, 실패하면 발행하지 않는지|
|`PaymentEventFlowTest`|실제 트랜잭션에서 결제 → 주문 확정 → 재고 반영 → 읽기 모델까지 이어지는지|
