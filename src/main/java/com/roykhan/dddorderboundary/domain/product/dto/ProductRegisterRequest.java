package com.roykhan.dddorderboundary.domain.product.dto;

import java.math.BigDecimal;

public record ProductRegisterRequest(
    String name,
    String description,
    BigDecimal price
) {}