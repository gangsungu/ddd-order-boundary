package com.roykhan.dddorderboundary.order.adapter.out.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.roykhan.dddorderboundary.order.application.port.out.ProductSnapshot;
import com.roykhan.dddorderboundary.product.application.port.in.ProductInfo;
import com.roykhan.dddorderboundary.product.application.port.in.ProductUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAdapter 단위 테스트")
class ProductAdapterTest {

    @Mock
    private ProductUseCase productUseCase;

    @InjectMocks
    private ProductAdapter productAdapter;

    @Test
    @DisplayName("상품 컨텍스트의 조회 결과를 주문 시점 스냅샷(ID·이름·단가)으로 옮긴다")
    void 스냅샷으로_변환() {
        given(productUseCase.findAllByIds(List.of(1L, 2L))).willReturn(List.of(
            new ProductInfo(1L, "글렌피딕 12년", "싱글몰트", new BigDecimal("89000.00")),
            new ProductInfo(2L, "맥캘란 12년", "셰리 캐스크", new BigDecimal("150000.00"))));

        List<ProductSnapshot> snapshots = productAdapter.findAll(List.of(1L, 2L));

        assertThat(snapshots).containsExactly(
            new ProductSnapshot(1L, "글렌피딕 12년", new BigDecimal("89000.00")),
            new ProductSnapshot(2L, "맥캘란 12년", new BigDecimal("150000.00")));
    }

    @Test
    @DisplayName("상품 컨텍스트가 찾지 못한 ID 는 결과에서 빠진 채로 돌려준다")
    void 없는_상품은_빠진다() {
        given(productUseCase.findAllByIds(List.of(1L, 99L)))
            .willReturn(List.of(new ProductInfo(1L, "글렌피딕 12년", "싱글몰트", new BigDecimal("89000.00"))));

        List<ProductSnapshot> snapshots = productAdapter.findAll(List.of(1L, 99L));

        assertThat(snapshots).extracting(ProductSnapshot::productId).containsExactly(1L);
    }
}
