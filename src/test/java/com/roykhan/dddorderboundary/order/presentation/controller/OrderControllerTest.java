package com.roykhan.dddorderboundary.order.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roykhan.dddorderboundary.common.config.JacksonConfig;
import com.roykhan.dddorderboundary.order.application.dto.CreateOrderCommand;
import com.roykhan.dddorderboundary.order.application.dto.OrderInfo;
import com.roykhan.dddorderboundary.order.application.usecase.OrderUseCase;
import com.roykhan.dddorderboundary.order.domain.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(JacksonConfig.class)
@DisplayName("OrderController 슬라이스 테스트")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @Test
    @DisplayName("POST /api/order - 요청을 커맨드로 바꿔 넘기고, 부여된 주문 ID 를 data 에 담아 반환한다")
    void 주문_생성() throws Exception {
        given(orderUseCase.createOrder(any(CreateOrderCommand.class))).willReturn(42L);

        mockMvc.perform(post("/api/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 7,
                      "items": [
                        { "productId": 1, "quantity": 2 },
                        { "productId": 3, "quantity": 1 }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("주문 생성에 성공했습니다."))
            .andExpect(jsonPath("$.data.orderId").value(42));

        ArgumentCaptor<CreateOrderCommand> captor = ArgumentCaptor.forClass(CreateOrderCommand.class);
        verify(orderUseCase).createOrder(captor.capture());
        CreateOrderCommand command = captor.getValue();
        assertThat(command.memberId()).isEqualTo(7L);
        assertThat(command.lines()).containsExactly(
            new CreateOrderCommand.Line(1L, 2),
            new CreateOrderCommand.Line(3L, 1));
    }

    @Test
    @DisplayName("POST /api/order - 주문 항목이 비어 있으면 VALIDATION_FAILED 로 실패한다")
    void 주문_항목_없음() throws Exception {
        mockMvc.perform(post("/api/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"memberId": 7, "items": []}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.data.items").value("주문 항목은 최소 1개 이상이어야 합니다."));

        verify(orderUseCase, never()).createOrder(any());
    }

    @Test
    @DisplayName("POST /api/order - 항목의 수량이 1 미만이면 VALIDATION_FAILED 로 실패한다")
    void 수량_1_미만() throws Exception {
        mockMvc.perform(post("/api/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"memberId": 7, "items": [{ "productId": 1, "quantity": 0 }]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.data['items[0].quantity']").value("수량은 1개 이상이어야 합니다."));

        verify(orderUseCase, never()).createOrder(any());
    }

    @Test
    @DisplayName("GET /api/order/{orderId} - 주문 상태와 주문 시점 항목 내역을 반환한다")
    void 주문_조회() throws Exception {
        OrderInfo info = new OrderInfo(
            1L, 7L, OrderStatus.PENDING, new BigDecimal("178000.00"),
            LocalDateTime.of(2026, 9, 22, 20, 0), null, null,
            List.of(new OrderInfo.Item(1L, "글렌피딕 12년", new BigDecimal("89000.00"), 2, new BigDecimal("178000.00"))));
        given(orderUseCase.findById(1L)).willReturn(info);

        mockMvc.perform(get("/api/order/{orderId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("주문을 조회하였습니다."))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.totalPrice").value(178000.00))
            .andExpect(jsonPath("$.data.items[0].productName").value("글렌피딕 12년"))
            .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("GET /api/order/{orderId} - 주문이 없으면 404 ORDER_NOT_FOUND 로 실패한다")
    void 주문_없음() throws Exception {
        given(orderUseCase.findById(99L)).willThrow(OrderErrorCode.ORDER_NOT_FOUND.exception());

        mockMvc.perform(get("/api/order/{orderId}", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/order/{orderId}/cancel - 유스케이스에 취소를 위임하고 data 없는 성공 응답을 반환한다")
    void 주문_취소() throws Exception {
        mockMvc.perform(patch("/api/order/{orderId}/cancel", 7L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("생성된 주문을 취소하였습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(orderUseCase).cancelOrder(7L);
    }

    @Test
    @DisplayName("PATCH /api/order/{orderId}/cancel - 확정된 주문이면 409 ORDER_ALREADY_CONFIRMED 로 실패한다")
    void 확정된_주문_취소() throws Exception {
        doThrow(OrderErrorCode.ORDER_ALREADY_CONFIRMED.exception()).when(orderUseCase).cancelOrder(7L);

        mockMvc.perform(patch("/api/order/{orderId}/cancel", 7L))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORDER_ALREADY_CONFIRMED"));
    }
}
