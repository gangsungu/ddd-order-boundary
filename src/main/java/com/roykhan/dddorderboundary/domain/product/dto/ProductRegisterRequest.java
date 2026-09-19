package com.roykhan.dddorderboundary.domain.product.dto;

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
    BigDecimal price
) {}