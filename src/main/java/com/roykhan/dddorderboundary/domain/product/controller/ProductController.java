package com.roykhan.dddorderboundary.domain.product.controller;

import com.roykhan.dddorderboundary.domain.product.dto.ProductInfo;
import com.roykhan.dddorderboundary.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    // 상품 정보 조회 (단건)
    @GetMapping("/product/{id}")
    @Operation(summary = "상품 정보 조회 (단건)", description = "주문 시점 스냅샷용")
    public ResponseEntity<ProductInfo> getProduct(@PathVariable Long id) {
        ProductInfo productInfo = productService.findById(id);
        return ResponseEntity.ok(productInfo);
    }

    // 상품 등록
    @PostMapping("/product")
    @Operation(summary = "상품 등록", description = "상품 등록")
    public ResponseEntity<Void> createProduct(@Valid @RequestBody ProductInfo productInfo) {
        productService.register(productInfo);
        return ResponseEntity.ok(productInfo);
    }

    // 상품 수정

    // 상품 삭제
}
