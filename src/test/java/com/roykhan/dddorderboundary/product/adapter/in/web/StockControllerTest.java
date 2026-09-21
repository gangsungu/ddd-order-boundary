package com.roykhan.dddorderboundary.product.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roykhan.dddorderboundary.common.config.JacksonConfig;
import com.roykhan.dddorderboundary.product.application.port.in.StockInfo;
import com.roykhan.dddorderboundary.product.application.port.in.StockUseCase;
import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockController.class)
@Import(JacksonConfig.class)
@DisplayName("StockController 슬라이스 테스트")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockUseCase stockUseCase;

    @Test
    @DisplayName("GET /api/product/{productId}/stock - 총 재고·가용·예약 수량을 data 에 담아 반환한다")
    void 재고_조회() throws Exception {
        given(stockUseCase.findByProductId(1L))
            .willReturn(new StockInfo(1L, 10, 8, 2, StockStatus.IN_STOCK));

        mockMvc.perform(get("/api/product/{productId}/stock", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("재고를 조회하였습니다."))
            .andExpect(jsonPath("$.data.productId").value(1))
            .andExpect(jsonPath("$.data.quantity").value(10))
            .andExpect(jsonPath("$.data.availableQuantity").value(8))
            .andExpect(jsonPath("$.data.reservedQuantity").value(2))
            .andExpect(jsonPath("$.data.stockStatus").value("IN_STOCK"));
    }

    @Test
    @DisplayName("GET /api/product/{productId}/stock - 재고가 없으면 404 STOCK_NOT_FOUND 로 실패한다")
    void 재고_없음() throws Exception {
        given(stockUseCase.findByProductId(99L)).willThrow(StockErrorCode.STOCK_NOT_FOUND.exception());

        mockMvc.perform(get("/api/product/{productId}/stock", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("STOCK_NOT_FOUND"));
    }
}
