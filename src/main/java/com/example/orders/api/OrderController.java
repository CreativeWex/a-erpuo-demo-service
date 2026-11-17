package com.example.orders.api;

import com.example.orders.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
    OrderResponse body =
        OrderResponse.from(
            service.create(request.customerName(), request.domainItems(), request.status()));
    return ResponseEntity.created(URI.create("/orders/" + body.id())).body(body);
  }

  @GetMapping
  public List<OrderResponse> list() {
    return service.list().stream().map(OrderResponse::from).toList();
  }

  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable UUID id) {
    return OrderResponse.from(service.get(id));
  }

  @PutMapping("/{id}")
  public OrderResponse replace(@PathVariable UUID id, @Valid @RequestBody OrderRequest request) {
    return OrderResponse.from(
        service.replace(id, request.customerName(), request.domainItems(), request.status()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
