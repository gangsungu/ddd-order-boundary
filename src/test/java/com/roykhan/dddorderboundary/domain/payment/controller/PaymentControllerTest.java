package com.roykhan.dddorderboundary.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roykhan.dddorderboundary.common.config.JacksonConfig;
import com.roykhan.dddorderboundary.common.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.domain.payment.enums.PaymentResult;
import com.roykhan.dddorderboundary.domain.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(JacksonConfig.class)
@DisplayName("PaymentController 슬라이스 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /api/payment/{orderId}/result - 경로의 주문 ID 와 결제 결과를 서비스에 넘긴다")
    void 결제_결과_반영() throws Exception {
        mockMvc.perform(post("/api/payment/{orderId}/result", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"result": "SUCCESS"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("결제 결과를 반영하였습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(paymentService).applyResult(7L, PaymentResult.SUCCESS);
    }

    @Test
    @DisplayName("POST /api/payment/{orderId}/result - 결과가 빠지면 VALIDATION_FAILED 로 실패한다")
    void 결제_결과_누락() throws Exception {
        mockMvc.perform(post("/api/payment/{orderId}/result", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.data.result").value("결제 결과는 필수입니다."));

        verify(paymentService, never()).applyResult(anyLong(), any());
    }

    @Test
    @DisplayName("POST /api/payment/{orderId}/result - SUCCESS·FAILURE 가 아닌 값은 INVALID_REQUEST 로 실패한다")
    void 결제_결과_잘못된_값() throws Exception {
        mockMvc.perform(post("/api/payment/{orderId}/result", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"result": "PENDING"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(paymentService, never()).applyResult(anyLong(), any());
    }

    @Test
    @DisplayName("POST /api/payment/{orderId}/result - 이미 확정된 주문이면 409 ORDER_ALREADY_CONFIRMED 로 실패한다")
    void 이미_확정된_주문() throws Exception {
        doThrow(OrderErrorCode.ORDER_ALREADY_CONFIRMED.exception())
            .when(paymentService).applyResult(7L, PaymentResult.SUCCESS);

        mockMvc.perform(post("/api/payment/{orderId}/result", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"result": "SUCCESS"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("ORDER_ALREADY_CONFIRMED"));
    }
}
