package com.roykhan.dddorderboundary.product.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRegisterRequest(
    @NotBlank
    String name,
    @NotBlank
    String description,
    @NotNull @Min(0)
    BigDecimal price,
    @NotNull @Min(0)
    int initialQuantity
) {}