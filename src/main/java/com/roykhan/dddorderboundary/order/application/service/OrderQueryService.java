package com.roykhan.dddorderboundary.order.application.service;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.in.OrderQueryUseCase;
import com.roykhan.dddorderboundary.order.application.port.out.OrderViewRepository;
import com.roykhan.dddorderboundary.order.exception.out.OrderLookupErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 읽기(Query) 쪽 - 읽기 모델을 그대로 돌려준다. 쓰기 모델(Order)과 재고는 건드리지 않는다
@Service
@RequiredArgsConstructor
public class OrderQueryService implements OrderQueryUseCase {

    private final OrderViewRepository orderViewRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderInfo findById(long orderId) {
        return orderViewRepository.findById(orderId)
            .orElseThrow(OrderLookupErrorCode.ORDER_NOT_FOUND::exception);
    }
}
