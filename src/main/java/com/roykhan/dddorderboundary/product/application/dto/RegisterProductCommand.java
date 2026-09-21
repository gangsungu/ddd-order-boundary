package com.roykhan.dddorderboundary.product.application.dto;

import java.math.BigDecimal;

public record RegisterProductCommand(
    String name,
    String description,
    BigDecimal price,
    int initialQuantity
) {}
