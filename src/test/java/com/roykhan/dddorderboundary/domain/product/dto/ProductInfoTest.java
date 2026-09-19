package com.roykhan.dddorderboundary.domain.product.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.roykhan.dddorderboundary.domain.product.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ProductInfo 변환")
class ProductInfoTest {

    @Test
    @DisplayName("엔티티의 id와 모든 필드를 그대로 옮긴다")
    void from_엔티티를_DTO로_변환한다() {
        Product product = Product.builder()
            .name("맥북 프로 14")
            .description("M4 Pro / 24GB / 512GB")
            .price(new BigDecimal("3290000.00"))
            .build();
        ReflectionTestUtils.setField(product, "id", 42L);

        ProductInfo info = ProductInfo.from(product);

        assertThat(info.id()).isEqualTo(42L);
        assertThat(info.name()).isEqualTo("맥북 프로 14");
        assertThat(info.description()).isEqualTo("M4 Pro / 24GB / 512GB");
        assertThat(info.price()).isEqualByComparingTo("3290000.00");
    }

    @Test
    @DisplayName("아직 저장되지 않은 엔티티는 id가 null인 채로 변환된다")
    void from_영속화_전이면_id가_null이다() {
        Product product = Product.builder()
            .name("신규 상품")
            .description("아직 저장 전")
            .price(BigDecimal.TEN)
            .build();

        ProductInfo info = ProductInfo.from(product);

        assertThat(info.id()).isNull();
        assertThat(info.name()).isEqualTo("신규 상품");
    }
}
