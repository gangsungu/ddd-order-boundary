package com.roykhan.dddorderboundary.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.common.exception.ProductErrorCode;
import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.dto.ProductInfo;
import com.roykhan.dddorderboundary.domain.product.dto.ProductRegisterRequest;
import com.roykhan.dddorderboundary.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private static Product product(Long id, String name, String description, String price) {
        Product product = Product.builder()
            .name(name)
            .description(description)
            .price(new BigDecimal(price))
            .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static ProductRegisterRequest request(String name, String description, String price) {
        return new ProductRegisterRequest(name, description, new BigDecimal(price));
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("상품이 있으면 ProductInfo로 변환해 반환한다")
        void 조회_성공() {
            given(productRepository.findById(1L))
                .willReturn(Optional.of(product(1L, "키보드", "무접점 45g", "159000.00")));

            ProductInfo info = productService.findById(1L);

            assertThat(info.id()).isEqualTo(1L);
            assertThat(info.name()).isEqualTo("키보드");
            assertThat(info.description()).isEqualTo("무접점 45g");
            assertThat(info.price()).isEqualByComparingTo("159000.00");
        }

        @Test
        @DisplayName("상품이 없으면 PRODUCT_NOT_FOUND로 실패한다")
        void 조회_실패() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductErrorCode.PRODUCT_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("같은 이름의 상품이 없으면 저장한다")
        void 등록_성공() {
            given(productRepository.existsByName("마우스")).willReturn(false);

            productService.register(request("마우스", "무선 경량", "89000.00"));

            verify(productRepository).save(productCaptor.capture());
            Product saved = productCaptor.getValue();
            assertThat(saved.getId()).isNull();
            assertThat(saved.getName()).isEqualTo("마우스");
            assertThat(saved.getDescription()).isEqualTo("무선 경량");
            assertThat(saved.getPrice()).isEqualByComparingTo("89000.00");
        }

        @Test
        @DisplayName("같은 이름의 상품이 이미 있으면 저장하지 않고 PRODUCT_ALREADY_EXIST로 실패한다")
        void 등록_중복() {
            given(productRepository.existsByName("마우스")).willReturn(true);

            assertThatThrownBy(() -> productService.register(request("마우스", "무선 경량", "89000.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductErrorCode.PRODUCT_ALREADY_EXIST.getMessage())
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.PRODUCT_ALREADY_EXIST);

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("조회한 엔티티의 필드를 변경한다 (더티 체킹이므로 save를 호출하지 않는다)")
        void 수정_성공() {
            Product existing = product(1L, "이전 이름", "이전 설명", "1000.00");
            given(productRepository.findById(1L)).willReturn(Optional.of(existing));

            productService.update(1L, request("새 이름", "새 설명", "2000.00"));

            assertThat(existing.getName()).isEqualTo("새 이름");
            assertThat(existing.getDescription()).isEqualTo("새 설명");
            assertThat(existing.getPrice()).isEqualByComparingTo("2000.00");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("상품이 없으면 PRODUCT_NOT_FOUND로 실패한다")
        void 수정_실패() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(99L, request("새 이름", "새 설명", "1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductErrorCode.PRODUCT_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("상품이 있으면 삭제한다")
        void 삭제_성공() {
            given(productRepository.existsById(1L)).willReturn(true);

            productService.delete(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("상품이 없으면 삭제하지 않고 PRODUCT_NOT_FOUND로 실패한다")
        void 삭제_실패() {
            given(productRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductErrorCode.PRODUCT_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);

            verify(productRepository, never()).deleteById(anyLong());
        }
    }
}
