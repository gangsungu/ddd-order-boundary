package com.roykhan.dddorderboundary.product.adapter.in.web;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import com.roykhan.dddorderboundary.product.application.port.in.ProductInfo;
import com.roykhan.dddorderboundary.product.application.port.in.ProductUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductUseCase productUseCase;

    // 상품 정보 조회 (단건)
    @GetMapping("/product/{id}")
    @Operation(summary = "상품 정보 조회 (단건)", description = "주문 시점 스냅샷용")
    public ApiResponse<ProductInfo> getProduct(@PathVariable Long id) {
        ProductInfo productInfo = productUseCase.findById(id);
        return ApiResponse.success("상품을 조회하였습니다.", productInfo);
    }

    // 상품 등록
    @PostMapping("/product")
    @Operation(summary = "상품 등록", description = "상품 등록")
    public ApiResponse<Void> createProduct(@Valid @RequestBody ProductRegisterRequest req) {
        productUseCase.register(req.toRegisterCommand());
        return ApiResponse.success("상품이 등록되었습니다.");
    }

    // 상품 수정
    @PutMapping("/product/{id}")
    @Operation(summary = "상품 수정", description = "상품 수정")
    public ApiResponse<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRegisterRequest req) {
        productUseCase.update(id, req.toUpdateCommand());
        return ApiResponse.success("상품 정보가 수정되었습니다.");
    }

    // 상품 삭제
    @DeleteMapping("/product/{id}")
    @Operation(summary = "상품 삭제", description = "상품 삭제")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productUseCase.delete(id);
        return ApiResponse.success("상품이 삭제되었습니다.");
    }
}
