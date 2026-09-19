package com.roykhan.dddorderboundary.domain.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roykhan.dddorderboundary.common.config.JacksonConfig;
import com.roykhan.dddorderboundary.domain.product.dto.ProductInfo;
import com.roykhan.dddorderboundary.domain.product.dto.ProductRegisterRequest;
import com.roykhan.dddorderboundary.domain.product.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(JacksonConfig.class)
@DisplayName("ProductController 슬라이스 테스트")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/product/{id} - 조회한 상품을 data에 담아 반환한다")
    void 상품_단건_조회() throws Exception {
        given(productService.findById(1L))
            .willReturn(new ProductInfo(1L, "키보드", "무접점 45g", new BigDecimal("159000.00")));

        mockMvc.perform(get("/api/product/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.message").value("상품을 조회하였습니다."))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("키보드"))
            .andExpect(jsonPath("$.data.description").value("무접점 45g"))
            .andExpect(jsonPath("$.data.price").value(159000.00));
    }

    @Test
    @DisplayName("POST /api/product - 요청 본문을 서비스에 그대로 넘기고, data 없는 성공 응답을 반환한다")
    void 상품_등록() throws Exception {
        mockMvc.perform(post("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "마우스",
                      "description": "무선 경량",
                      "price": 89000.00
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("상품이 등록되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<ProductRegisterRequest> captor = ArgumentCaptor.forClass(ProductRegisterRequest.class);
        verify(productService).register(captor.capture());
        ProductRegisterRequest passed = captor.getValue();
        assertThat(passed.name()).isEqualTo("마우스");
        assertThat(passed.description()).isEqualTo("무선 경량");
        assertThat(passed.price()).isEqualByComparingTo("89000.00");
    }

    @Test
    @DisplayName("PUT /api/product/{id} - 경로의 id와 본문을 서비스에 넘긴다")
    void 상품_수정() throws Exception {
        mockMvc.perform(put("/api/product/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "새 이름",
                      "description": "새 설명",
                      "price": 12345.00
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("상품 정보가 수정되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<ProductRegisterRequest> captor = ArgumentCaptor.forClass(ProductRegisterRequest.class);
        verify(productService).update(org.mockito.ArgumentMatchers.eq(7L), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("새 이름");
    }

    @Test
    @DisplayName("DELETE /api/product/{id} - 서비스에 삭제를 위임한다")
    void 상품_삭제() throws Exception {
        mockMvc.perform(delete("/api/product/{id}", 3L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("상품이 삭제되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(productService).delete(3L);
    }
}
