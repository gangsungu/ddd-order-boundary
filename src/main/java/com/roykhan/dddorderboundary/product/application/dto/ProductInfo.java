package com.roykhan.dddorderboundary.product.application.dto;

import com.roykhan.dddorderboundary.product.domain.model.Product;
import java.math.BigDecimal;

public record ProductInfo(
    Long id,
    String name,
    String description,
    BigDecimal price
) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice()
        );
    }
}