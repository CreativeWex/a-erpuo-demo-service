package com.example.orders.repository;

import com.example.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Хранилище заказов. */
public interface OrderRepository {

  Order save(Order order);

  Optional<Order> findById(UUID id);

  /** Все заказы по возрастанию createdAt, при равенстве — в порядке создания. */
  List<Order> findAll();

  /** Атомарно заменяет существующий заказ; пусто, если заказа нет. */
  Optional<Order> replace(UUID id, UnaryOperator<Order> updater);

  /** Удаляет заказ; возвращает true, если заказ существовал. */
  boolean deleteById(UUID id);
}
