package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class OrderDeleteApiTest extends AbstractOrdersApiTest {

  @Test
  @DisplayName("Удаление существующего заказа: 204 без тела, затем чтение даёт 404")
  void deleteRemovesOrder() throws Exception {
    String id = createOrder(validOrderJson()).path("id").asText();

    MvcResult result =
        mockMvc.perform(delete("/orders/" + id)).andExpect(status().isNoContent()).andReturn();

    assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
    mockMvc.perform(get("/orders/" + id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Удаление удаляет только указанный заказ, остальные остаются")
  void deleteKeepsOtherOrders() throws Exception {
    String keptId = createOrder(validOrderJson()).path("id").asText();
    String removedId = createOrder(validOrderJson()).path("id").asText();

    mockMvc.perform(delete("/orders/" + removedId)).andExpect(status().isNoContent());

    JsonNode orders = listOrders();
    assertThat(orders).hasSize(1);
    assertThat(orders.get(0).path("id").asText()).isEqualTo(keptId);
  }

  @Test
  @DisplayName("Удаление несуществующего заказа даёт 404")
  void deleteUnknownOrderReturnsNotFound() throws Exception {
    // Предусловие: эндпоинт существует (иначе 404 был бы тривиальным) — другой заказ удаляется.
    String existingId = createOrder(validOrderJson()).path("id").asText();
    mockMvc.perform(delete("/orders/" + existingId)).andExpect(status().isNoContent());

    mockMvc.perform(delete("/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Повторное удаление уже удалённого заказа даёт 404")
  void deleteTwiceReturnsNotFoundSecondTime() throws Exception {
    String id = createOrder(validOrderJson()).path("id").asText();
    mockMvc.perform(delete("/orders/" + id)).andExpect(status().isNoContent());

    mockMvc.perform(delete("/orders/" + id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE с идентификатором в пути, не являющимся UUID, даёт 400, а не 404")
  void deleteWithNonUuidIdReturnsBadRequest() throws Exception {
    mockMvc.perform(delete("/orders/not-a-uuid")).andExpect(status().isBadRequest());
  }
}
