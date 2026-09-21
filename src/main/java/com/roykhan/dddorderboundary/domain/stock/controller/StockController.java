package com.roykhan.dddorderboundary.domain.stock.controller;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import com.roykhan.dddorderboundary.domain.stock.dto.StockInfo;
import com.roykhan.dddorderboundary.domain.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 재고는 상품 컨텍스트가 소유하므로 상품 경로 아래에 둔다
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/product/{productId}/stock")
    @Operation(summary = "재고 조회", description = "총 재고, 가용 수량, 예약 중인 수량을 조회")
    public ApiResponse<StockInfo> getStock(@PathVariable Long productId) {
        StockInfo stockInfo = stockService.findByProductId(productId);
        return ApiResponse.success("재고를 조회하였습니다.", stockInfo);
    }
}
