package com.example.orders.api;

import com.example.orders.domain.OrderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record OrderItemRequest(
    @NotBlank String productName,
    @Positive int quantity,
    @NotNull @PositiveOrZero BigDecimal price) {

  OrderItem toDomain() {
    return new OrderItem(productName, quantity, price);
  }
}
