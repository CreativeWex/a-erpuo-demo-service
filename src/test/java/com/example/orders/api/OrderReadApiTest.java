package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class OrderReadApiTest extends AbstractOrdersApiTest {

  @Test
  @DisplayName("Список возвращает все заказы по возрастанию времени создания")
  void listReturnsOrdersInCreationOrder() throws Exception {
    List<String> expectedIds = new ArrayList<>();
    for (String name : List.of("First", "Second", "Third")) {
      expectedIds.add(createOrder(orderJson(name, "[" + VALID_ITEM + "]")).path("id").asText());
    }

    JsonNode orders = listOrders();

    assertThat(orders).hasSize(3);
    List<String> actualIds = new ArrayList<>();
    List<Instant> createdAt = new ArrayList<>();
    orders.forEach(
        o -> {
          actualIds.add(o.path("id").asText());
          createdAt.add(Instant.parse(o.path("createdAt").asText()));
        });
    assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    assertThat(createdAt).isSorted();
  }

  @Test
  @DisplayName("Когда заказов нет, список — 200 и пустой массив")
  void listIsEmptyWhenNoOrders() throws Exception {
    MvcResult result = mockMvc.perform(get("/orders")).andExpect(status().isOk()).andReturn();

    JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
    assertThat(body.isArray()).isTrue();
    assertThat(body).isEmpty();
  }

  @Test
  @DisplayName("Существующий заказ читается по идентификатору: 200 и его данные")
  void getReturnsExistingOrder() throws Exception {
    JsonNode created = createOrder(validOrderJson());
    String id = created.path("id").asText();

    MvcResult result = mockMvc.perform(get("/orders/" + id)).andExpect(status().isOk()).andReturn();

    JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
    assertThat(body.path("id").asText()).isEqualTo(id);
    assertThat(body.path("customerName").asText()).isEqualTo("Alice");
    assertThat(body.path("status").asText()).isEqualTo("NEW");
    assertThat(body.path("createdAt").asText()).isEqualTo(created.path("createdAt").asText());
    assertThat(body.path("items")).hasSize(1);
  }

  @Test
  @DisplayName("Заказ с неизвестным, но корректным UUID не найден: 404")
  void getUnknownOrderReturnsNotFound() throws Exception {
    // Предусловие: эндпоинт существует (иначе 404 был бы тривиальным) — другой заказ читается.
    String existingId = createOrder(validOrderJson()).path("id").asText();
    mockMvc.perform(get("/orders/" + existingId)).andExpect(status().isOk());

    mockMvc.perform(get("/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Идентификатор в пути, не являющийся UUID, даёт 400, а не 404")
  void getWithNonUuidIdReturnsBadRequest() throws Exception {
    mockMvc.perform(get("/orders/not-a-uuid")).andExpect(status().isBadRequest());
  }
}
