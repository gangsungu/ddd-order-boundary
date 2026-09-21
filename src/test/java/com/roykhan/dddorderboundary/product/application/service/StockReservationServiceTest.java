package com.roykhan.dddorderboundary.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.product.domain.exception.ReservationErrorCode;
import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.ReservationStatus;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.model.StockReservation;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import com.roykhan.dddorderboundary.product.domain.repository.StockReservationRepository;
import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("StockReservationService 단위 테스트")
class StockReservationServiceTest {

    private static final LocalDateTime LATER = LocalDateTime.now().plusMinutes(10);

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @InjectMocks
    private StockReservationService stockReservationService;

    @Captor
    private ArgumentCaptor<StockReservation> reservationCaptor;

    // 예약이 걸린 상태를 만든다. 생성과 동시에 가용 수량이 예약으로 넘어간다
    private void givenReservations(Long orderId, StockReservation... reservations) {
        given(stockReservationRepository.findAllByOrderId(orderId)).willReturn(List.of(reservations));
    }

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("가용 수량을 예약으로 옮기고 주문 ID 로 묶인 예약을 저장한다")
        void 예약_성공() {
            Stock stock = Stock.create(1L, 10);
            given(stockRepository.findByProductId(1L)).willReturn(Optional.of(stock));

            stockReservationService.reserve(42L, 1L, 3, LATER);

            assertThat(stock.getQuantity()).isEqualTo(10);
            assertThat(stock.getAvailableQuantity()).isEqualTo(7);

            verify(stockReservationRepository).save(reservationCaptor.capture());
            StockReservation saved = reservationCaptor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(42L);
            assertThat(saved.getStock()).isSameAs(stock);
            assertThat(saved.getReservedQuantity()).isEqualTo(3);
            assertThat(saved.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(saved.getExpireAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("재고가 없으면 STOCK_NOT_FOUND 로 실패하고 예약을 저장하지 않는다")
        void 재고_없음() {
            given(stockRepository.findByProductId(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stockReservationService.reserve(42L, 99L, 1, LATER))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(StockErrorCode.STOCK_NOT_FOUND);

            verify(stockReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("가용 수량보다 많이 예약하면 OUT_OF_STOCK 으로 실패하고 재고를 건드리지 않는다")
        void 재고_부족() {
            Stock stock = Stock.create(1L, 2);
            given(stockRepository.findByProductId(1L)).willReturn(Optional.of(stock));

            assertThatThrownBy(() -> stockReservationService.reserve(42L, 1L, 3, LATER))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(StockErrorCode.OUT_OF_STOCK);

            assertThat(stock.getAvailableQuantity()).isEqualTo(2);
            verify(stockReservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("주문의 예약을 모두 확정하고 예약한 수량만큼 총 재고를 차감한다")
        void 확정_성공() {
            Stock first = Stock.create(1L, 10);
            Stock second = Stock.create(2L, 5);
            StockReservation a = StockReservation.create(first, 42L, 3, LATER);
            StockReservation b = StockReservation.create(second, 42L, 2, LATER);
            givenReservations(42L, a, b);

            stockReservationService.confirm(42L);

            assertThat(a.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(b.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(a.getConfirmedAt()).isNotNull();

            // 가용 수량은 예약 시점에 이미 빠졌으므로 그대로이고, 총 재고가 따라 내려온다
            assertThat(first.getQuantity()).isEqualTo(7);
            assertThat(first.getAvailableQuantity()).isEqualTo(7);
            assertThat(first.reservedQuantity()).isZero();
            assertThat(second.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("결제 마감이 지난 예약은 RESERVATION_EXPIRED 로 실패하고 총 재고를 차감하지 않는다")
        void 결제_마감_지남() {
            Stock stock = Stock.create(1L, 10);
            givenReservations(42L, StockReservation.create(stock, 42L, 3, LocalDateTime.now().minusMinutes(1)));

            assertThatThrownBy(() -> stockReservationService.confirm(42L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.RESERVATION_EXPIRED);

            assertThat(stock.getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("이미 해제된 예약은 RESERVATION_NOT_CONFIRMABLE 로 실패한다")
        void 해제된_예약() {
            Stock stock = Stock.create(1L, 10);
            StockReservation reservation = StockReservation.create(stock, 42L, 3, LATER);
            reservation.cancel();
            givenReservations(42L, reservation);

            assertThatThrownBy(() -> stockReservationService.confirm(42L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_CONFIRMABLE);

            assertThat(stock.getQuantity()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("주문의 예약을 해제해 CANCELLED 로 남기고 가용 수량을 돌려준다")
        void 해제_성공() {
            Stock stock = Stock.create(1L, 10);
            StockReservation reservation = StockReservation.create(stock, 42L, 3, LATER);
            givenReservations(42L, reservation);

            stockReservationService.cancel(42L);

            assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCancelledAt()).isNotNull();
            assertThat(stock.getQuantity()).isEqualTo(10);
            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("이미 해제된 예약은 RESERVATION_NOT_CANCELLABLE 로 실패하고 가용 수량을 다시 늘리지 않는다")
        void 중복_해제() {
            Stock stock = Stock.create(1L, 10);
            StockReservation reservation = StockReservation.create(stock, 42L, 3, LATER);
            reservation.cancel();
            givenReservations(42L, reservation);

            assertThatThrownBy(() -> stockReservationService.cancel(42L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_CANCELLABLE);

            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("결제 마감이 지난 예약도 해제할 수 있다")
        void 마감_지난_예약_해제() {
            Stock stock = Stock.create(1L, 10);
            StockReservation reservation = StockReservation.create(stock, 42L, 3, LocalDateTime.now().minusMinutes(1));
            givenReservations(42L, reservation);

            stockReservationService.cancel(42L);

            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("예약이 없는 주문이면 아무것도 하지 않는다")
        void 예약_없음() {
            givenReservations(42L);

            stockReservationService.cancel(42L);
        }
    }

    @Nested
    @DisplayName("expire")
    class Expire {

        @Test
        @DisplayName("주문의 예약을 만료시켜 EXPIRED 로 남기고 가용 수량을 돌려준다")
        void 만료_성공() {
            Stock stock = Stock.create(1L, 10);
            StockReservation reservation = StockReservation.create(stock, 42L, 3, LocalDateTime.now().minusMinutes(1));
            givenReservations(42L, reservation);

            stockReservationService.expire(42L);

            assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.EXPIRED);
            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        }
    }
}
