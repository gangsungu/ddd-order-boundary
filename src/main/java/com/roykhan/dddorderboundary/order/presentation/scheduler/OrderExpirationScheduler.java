package com.roykhan.dddorderboundary.order.presentation.scheduler;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.order.application.usecase.OrderUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 결제 마감이 지나도록 결제되지 않은 주문을 만료시켜 예약한 재고를 돌려준다
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    // 한 주기에 만료시킬 최대 주문 수. 남은 주문은 다음 주기에 처리한다
    private static final int BATCH_SIZE = 100;

    private final OrderUseCase orderUseCase;

    @Scheduled(
        initialDelayString = "${order.reservation.expire-check-interval:30}",
        fixedDelayString = "${order.reservation.expire-check-interval:30}",
        timeUnit = TimeUnit.SECONDS
    )
    public void expireOrders() {
        List<Long> orderIds = orderUseCase.findExpiredOrderIds(LocalDateTime.now(), BATCH_SIZE);

        // 주문마다 트랜잭션을 따로 잡아 한 건이 실패해도 나머지는 만료시킨다
        for (Long orderId : orderIds) {
            try {
                orderUseCase.expireOrder(orderId);
                log.info("주문 만료: orderId={}", orderId);
            } catch (BusinessException e) {
                // ID 를 읽은 뒤 결제·취소가 먼저 끝난 주문
                log.info("주문 만료 건너뜀: orderId={}, code={}", orderId, e.getErrorCode().name());
            } catch (OptimisticLockingFailureException e) {
                // 결제·취소가 같은 재고를 동시에 바꿨다. 주문이 아직 PENDING 이면 다음 주기에 다시 잡힌다
                log.warn("주문 만료 충돌, 다음 주기에 재시도: orderId={}", orderId);
            } catch (RuntimeException e) {
                log.error("주문 만료 실패: orderId={}", orderId, e);
            }
        }
    }
}
