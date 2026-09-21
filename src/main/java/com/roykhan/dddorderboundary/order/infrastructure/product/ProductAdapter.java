package com.roykhan.dddorderboundary.order.infrastructure.product;

import com.roykhan.dddorderboundary.order.application.port.ProductPort;
import com.roykhan.dddorderboundary.order.application.port.ProductSnapshot;
import com.roykhan.dddorderboundary.product.application.usecase.ProductUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 출력 어댑터 - 주문의 상품 포트를 상품 컨텍스트의 유스케이스 호출로 구현한다.
// 상품 쪽 표현(ProductInfo)을 주문 쪽 표현(ProductSnapshot)으로 옮기는 일도 여기서 한다
@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductPort {

    private final ProductUseCase productUseCase;

    @Override
    public List<ProductSnapshot> findAll(List<Long> productIds) {
        return productUseCase.findAllByIds(productIds).stream()
            .map(product -> new ProductSnapshot(product.id(), product.name(), product.price()))
            .toList();
    }
}
