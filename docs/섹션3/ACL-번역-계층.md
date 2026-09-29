## ACL — 번역 계층

### 왜 필요했나
|전|후|
|---|---|
|재고 포트가 상품 컨텍스트의 예외를 그대로 통과시켜 `OUT_OF_STOCK` · `RESERVATION_EXPIRED` 가 주문 API 응답까지 나갔다|주문 API 는 주문의 코드(`STOCK_NOT_ENOUGH` 등)만 내보낸다|
|결제가 주문 유스케이스를 불러, 주문이 거절하면 `ORDER_ALREADY_CONFIRMED` 가 결제 API 응답으로 나갔다|결제는 이벤트만 발행하고, 주문의 거절은 주문 안에서 끝난다|

+ 경계를 코드로 나눠도 상대의 **언어**(에러 코드, 이벤트 모양)가 넘어오면 결합이 남는다
    - 상품이 코드 이름을 바꾸면 주문 API 의 응답 계약이 바뀐다
+ 그래서 다른 컨텍스트와 만나는 자리마다 번역을 두고, 번역하는 곳을 한 군데로 모았다

### 번역하는 자리
|방향|자리|번역하는 것|
|---|---|---|
|주문 → 상품 (출력)|`order/adapter/out/product/acl/StockErrorTranslator`|상품의 에러 코드 → 주문의 `StockPortErrorCode`|
|주문 → 상품 (출력)|`order/adapter/out/product/StockAdapter` · `ProductAdapter`|주문의 요청(`StockLine`) → 상품의 커맨드, 상품 조회 결과 → `ProductSnapshot`|
|결제 → 주문 (입력)|`order/adapter/in/payment/PaymentEventListener`|결제 이벤트(`PaymentSucceeded` · `PaymentFailed`) → 주문 유스케이스 호출|

+ 주문 안에서 다른 컨텍스트를 import 하는 파일은 위 네 개뿐이고, 모두 `adapter` 에 있다
    - `domain` · `application` 에는 다른 컨텍스트 import 가 없다
    - 결제·상품 컨텍스트는 다른 컨텍스트를 하나도 import 하지 않는다

### 재고 예외 번역표
|상품 컨텍스트가 던지는 코드|주문이 받는 코드|
|---|---|
|`OUT_OF_STOCK`|`STOCK_NOT_ENOUGH`|
|`INVALID_QUANTITY` · `STOCK_NOT_FOUND` · `PRODUCT_NOT_FOUND`|`ORDER_ITEM_NOT_ORDERABLE`|
|`RESERVATION_EXPIRED`|`STOCK_RESERVATION_EXPIRED`|
|`RESERVATION_NOT_CONFIRMABLE` · `RESERVATION_NOT_CANCELLABLE`|`STOCK_RESERVATION_NOT_CHANGEABLE`|
|번역표에 없는 코드|`STOCK_PORT_FAILED` — 원본 코드는 로그로만 남긴다|

+ 모르는 코드를 통과시키면 경계가 뚫리므로 주문 쪽 일반 실패로 덮는다
+ 상품의 코드 여러 개를 주문 하나로 묶었다. 주문 입장에서는 "주문할 수 없는 항목"이면 충분하고, 재고 행이 없는지 상품이 없는지는 상품의 사정이다

### 결제 이벤트 번역
|결제의 사실|주문이 하는 일|
|---|---|
|`PaymentSucceeded`|`confirmOrder` — 주문 확정, 예약 확정|
|`PaymentFailed`|`failPayment` — 결제 실패, 예약 해제|
|주문이 거절 (이미 취소·만료 등)|결제로 되돌리지 않고 로그만 남긴다|

+ 결제는 "결제가 성공했다"는 사실만 말하고, 그 결과로 무엇을 할지는 주문이 정한다
+ `PaymentEvent` 는 `sealed` 라, 결제가 이벤트 종류를 늘리면 리스너의 `switch` 가 컴파일되지 않아 번역 누락을 막는다

### 결정
+ **ACL 을 출력 방향부터 시작했다**
    - 결제 → 주문의 `OrderAdapter` 는 이벤트 전환(EDA)에서 사라질 것이라, 거기에 번역을 먼저 넣으면 그대로 버려진다
    - 주문 → 상품은 전환 뒤에도 동기 호출로 남는다
+ **이벤트 계약은 발행하는 쪽이 소유하고, 받는 쪽이 번역한다**
    - `common/event` 같은 공유 커널에 두면 모든 컨텍스트가 같은 모양에 묶이고, 번역할 자리가 사라진다
+ **번역 대상 코드를 발생 지점별로 모아 둔 것이 도움이 됐다**
    - 섹션 2 코드리뷰로 나눈 `exception/out` 이 그대로 "포트가 실패했을 때 주문이 쓰는 코드"의 자리가 됐다

### 한계
+ 번역기가 상품의 에러 코드 enum 을 import 한다. 같은 JVM 이라 가능한 형태이고, 섹션 4 에서 네트워크를 건너면 응답 코드 문자열을 번역하게 된다
    - 번역하는 자리(`acl` 패키지)는 그대로 남는다
+ 결제 성공인데 주문이 받지 못한 경우의 보상(환불)은 아직 없다. Saga 에서 다룬다

### 테스트
|테스트|확인하는 것|
|---|---|
|`StockErrorTranslatorTest`|번역표의 모든 코드와 번역표에 없는 코드|
|`StockAdapterTest`|재고 유스케이스 호출이 번역기를 거치는지|
|`PaymentEventListenerTest`|이벤트를 어떤 유스케이스로 옮기는지, 주문의 거절을 밖으로 던지지 않는지|
