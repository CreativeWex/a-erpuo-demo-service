package com.example.orders.api;

import com.example.orders.domain.OrderItem;
import com.example.orders.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
    @NotBlank String customerName,
    @NotEmpty List<@Valid @NotNull OrderItemRequest> items,
    OrderStatus status) {

  List<OrderItem> domainItems() {
    return items.stream().map(OrderItemRequest::toDomain).toList();
  }
}
