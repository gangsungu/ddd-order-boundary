package com.roykhan.dddorderboundary.order.application.port.out;

import java.util.List;

// 출력 포트 - 주문이 상품 컨텍스트에서 읽어 오는 것.
// 주문 시점 스냅샷에 필요한 이름·단가만 주문 쪽 표현(ProductSnapshot)으로 받는다
public interface ProductPort {

    // 없는 ID 는 결과에서 빠진다. 빠진 게 있는지는 주문이 판단한다
    List<ProductSnapshot> findAll(List<Long> productIds);
}
