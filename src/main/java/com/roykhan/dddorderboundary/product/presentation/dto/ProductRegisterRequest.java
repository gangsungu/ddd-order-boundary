package com.roykhan.dddorderboundary.product.presentation.dto;

import com.roykhan.dddorderboundary.product.application.dto.RegisterProductCommand;
import com.roykhan.dddorderboundary.product.application.dto.UpdateProductCommand;
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
) {
    public RegisterProductCommand toRegisterCommand() {
        return new RegisterProductCommand(name, description, price, initialQuantity);
    }

    // 수정 요청도 같은 DTO 를 쓰지만 재고 수량은 상품 수정으로 바뀌지 않으므로 넘기지 않는다
    public UpdateProductCommand toUpdateCommand() {
        return new UpdateProductCommand(name, description, price);
    }
}
