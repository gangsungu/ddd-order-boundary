package com.roykhan.dddorderboundary.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.product.application.port.in.ProductInfo;
import com.roykhan.dddorderboundary.product.application.port.in.RegisterProductCommand;
import com.roykhan.dddorderboundary.product.application.port.in.UpdateProductCommand;
import com.roykhan.dddorderboundary.product.application.port.out.ProductRepository;
import com.roykhan.dddorderboundary.product.application.port.out.StockRepository;
import com.roykhan.dddorderboundary.product.domain.exception.ProductErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.Product;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import java.math.BigDecimal;
import java.util.List;
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

// 저장소 포트를 목으로 두고 유스케이스 규칙만 본다
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductApplicationService 단위 테스트")
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductApplicationService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Captor
    private ArgumentCaptor<Stock> stockCaptor;

    private static Product product(Long id, String name, String description, String price) {
        Product product = Product.builder()
            .name(name)
            .description(description)
            .price(new BigDecimal(price))
            .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static RegisterProductCommand registerCommand(String name, String description, String price, int initialQuantity) {
        return new RegisterProductCommand(name, description, new BigDecimal(price), initialQuantity);
    }

    private static UpdateProductCommand updateCommand(String name, String description, String price) {
        return new UpdateProductCommand(name, description, new BigDecimal(price));
    }

    // 저장 시점에 식별자가 부여되는 JPA 동작을 흉내낸다. 재고가 상품 ID 로 묶이므로 필요하다
    private void givenProductSaveAssignsId(long id) {
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", id);
            return product;
        });
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
    @DisplayName("findAllByIds")
    class FindAllByIds {

        @Test
        @DisplayName("요청한 ID 중 있는 상품만 ProductInfo로 변환해 반환한다")
        void 여러_건_조회() {
            given(productRepository.findAllById(List.of(1L, 2L, 99L)))
                .willReturn(List.of(product(1L, "키보드", "무접점", "1000"), product(2L, "마우스", "무선", "2000")));

            List<ProductInfo> infos = productService.findAllByIds(List.of(1L, 2L, 99L));

            assertThat(infos).extracting(ProductInfo::id).containsExactly(1L, 2L);
            assertThat(infos).extracting(ProductInfo::name).containsExactly("키보드", "마우스");
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("같은 이름의 상품이 없으면 저장하고 부여된 상품 ID 를 반환한다")
        void 등록_성공() {
            given(productRepository.existsByName("마우스")).willReturn(false);
            givenProductSaveAssignsId(7L);

            Long productId = productService.register(registerCommand("마우스", "무선 경량", "89000.00", 10));

            assertThat(productId).isEqualTo(7L);
            verify(productRepository).save(productCaptor.capture());
            Product saved = productCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("마우스");
            assertThat(saved.getDescription()).isEqualTo("무선 경량");
            assertThat(saved.getPrice()).isEqualByComparingTo("89000.00");
        }

        @Test
        @DisplayName("상품을 저장하면 요청한 초기 수량으로 재고도 함께 만든다")
        void 등록_재고_생성() {
            given(productRepository.existsByName("마우스")).willReturn(false);
            givenProductSaveAssignsId(7L);

            productService.register(registerCommand("마우스", "무선 경량", "89000.00", 25));

            verify(stockRepository).save(stockCaptor.capture());
            Stock stock = stockCaptor.getValue();
            assertThat(stock.getProductId()).isEqualTo(7L);
            assertThat(stock.getQuantity()).isEqualTo(25);
            assertThat(stock.getAvailableQuantity()).isEqualTo(25);
        }

        @Test
        @DisplayName("같은 이름의 상품이 이미 있으면 저장하지 않고 PRODUCT_ALREADY_EXIST로 실패한다")
        void 등록_중복() {
            given(productRepository.existsByName("마우스")).willReturn(true);

            assertThatThrownBy(() -> productService.register(registerCommand("마우스", "무선 경량", "89000.00", 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductErrorCode.PRODUCT_ALREADY_EXIST.getMessage())
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.PRODUCT_ALREADY_EXIST);

            verify(productRepository, never()).save(any());
            verify(stockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("조회한 상품을 update 로 바꾼다 (더티 체킹이므로 save를 호출하지 않는다)")
        void 수정_성공() {
            Product existing = product(1L, "이전 이름", "이전 설명", "1000.00");
            given(productRepository.findById(1L)).willReturn(Optional.of(existing));

            productService.update(1L, updateCommand("새 이름", "새 설명", "2000.00"));

            assertThat(existing.getName()).isEqualTo("새 이름");
            assertThat(existing.getDescription()).isEqualTo("새 설명");
            assertThat(existing.getPrice()).isEqualByComparingTo("2000.00");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("상품이 없으면 PRODUCT_NOT_FOUND로 실패한다")
        void 수정_실패() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(99L, updateCommand("새 이름", "새 설명", "1")))
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
