package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class OrderCreateApiTest extends AbstractOrdersApiTest {

  @Test
  @DisplayName("Валидный заказ без статуса создаётся: 201, Location, id, createdAt, статус NEW")
  void createsOrderWithDefaultStatus() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/orders").contentType(MediaType.APPLICATION_JSON).content(validOrderJson()))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
    String id = body.path("id").asText();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(result.getResponse().getHeader("Location")).endsWith("/orders/" + id);
    assertThat(body.hasNonNull("createdAt")).isTrue();
    assertThat(body.path("customerName").asText()).isEqualTo("Alice");
    assertThat(body.path("status").asText()).isEqualTo("NEW");
    assertThat(body.path("items")).hasSize(1);
    JsonNode item = body.path("items").get(0);
    assertThat(item.path("productName").asText()).isEqualTo("Widget");
    assertThat(item.path("quantity").asInt()).isEqualTo(2);
    assertThat(item.path("price").decimalValue()).isEqualByComparingTo("10.50");
  }

  @Test
  @DisplayName("id и createdAt из тела запроса при создании игнорируются, назначает сервис")
  void createIgnoresClientAssignedIdAndCreatedAt() throws Exception {
    String clientId = "11111111-1111-1111-1111-111111111111";
    String body =
        "{\"id\":\""
            + clientId
            + "\",\"createdAt\":\"2000-01-01T00:00:00Z\",\"customerName\":\"Alice\","
            + "\"items\":["
            + VALID_ITEM
            + "]}";

    JsonNode created = createOrder(body);

    assertThat(created.path("id").asText()).isNotEqualTo(clientId);
    assertThat(created.path("createdAt").asText()).doesNotStartWith("2000-01-01");
  }

  @ParameterizedTest(name = "[{index}] тело: {0}")
  @ValueSource(strings = {"", "   ", "{not a json", "[1,2", "plain text"})
  @DisplayName("Пустое или не являющееся JSON тело при создании отклоняется 400, заказ не создаётся")
  void rejectsEmptyOrMalformedBody(String body) throws Exception {
    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());

    assertNoOrdersExist();
  }

  @Test
  @DisplayName("Запрос создания без тела вообще отклоняется 400, заказ не создаётся")
  void rejectsRequestWithoutBody() throws Exception {
    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    assertNoOrdersExist();
  }

  @ParameterizedTest(name = "[{index}] тело: {0}")
  @ValueSource(
      strings = {
        "{\"items\":[{\"productName\":\"Widget\",\"quantity\":2,\"price\":10.50}]}",
        "{\"customerName\":null,\"items\":[{\"productName\":\"Widget\",\"quantity\":2,\"price\":10.50}]}",
        "{\"customerName\":\"\",\"items\":[{\"productName\":\"Widget\",\"quantity\":2,\"price\":10.50}]}"
      })
  @DisplayName("Заказ без имени клиента (нет, null, пусто) отклоняется 400, заказ не создаётся")
  void rejectsMissingCustomerName(String body) throws Exception {
    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());

    assertNoOrdersExist();
  }

  @ParameterizedTest(name = "[{index}] тело: {0}")
  @ValueSource(
      strings = {
        "{\"customerName\":\"Alice\"}",
        "{\"customerName\":\"Alice\",\"items\":null}",
        "{\"customerName\":\"Alice\",\"items\":[]}"
      })
  @DisplayName("Заказ без позиций (нет, null, пустой список) отклоняется 400, заказ не создаётся")
  void rejectsMissingItems(String body) throws Exception {
    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());

    assertNoOrdersExist();
  }

  @ParameterizedTest(name = "[{index}] позиция: {0}")
  @ValueSource(
      strings = {
        "{\"productName\":\"\",\"quantity\":1,\"price\":1.00}",
        "{\"productName\":\"Widget\",\"quantity\":0,\"price\":1.00}",
        "{\"productName\":\"Widget\",\"quantity\":-3,\"price\":1.00}",
        "{\"productName\":\"Widget\",\"quantity\":1,\"price\":-0.01}"
      })
  @DisplayName(
      "Некорректная позиция (пустой товар, количество <= 0, отрицательная цена) отклоняется 400")
  void rejectsInvalidItem(String item) throws Exception {
    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("Alice", "[" + item + "]")))
        .andExpect(status().isBadRequest());

    assertNoOrdersExist();
  }

  @Test
  @DisplayName("Позиция с нулевой ценой допустима (граница: цена неотрицательна)")
  void acceptsZeroPrice() throws Exception {
    JsonNode created =
        createOrder(
            orderJson("Alice", "[{\"productName\":\"Gift\",\"quantity\":1,\"price\":0}]"));

    assertThat(created.path("items").get(0).path("price").decimalValue())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Неизвестный статус при создании отклоняется 400, заказ не создаётся")
  void rejectsUnknownStatus() throws Exception {
    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("Alice", "[" + VALID_ITEM + "]", "SHIPPED")))
        .andExpect(status().isBadRequest());

    assertNoOrdersExist();
  }

  @Test
  @DisplayName("Явно переданный допустимый статус сохраняется при создании")
  void keepsExplicitStatus() throws Exception {
    JsonNode created = createOrder(orderJson("Alice", "[" + VALID_ITEM + "]", "CONFIRMED"));

    assertThat(created.path("status").asText()).isEqualTo("CONFIRMED");
  }

  @Test
  @DisplayName("Цена 10.50 после создания и последующего чтения равна 10.50 без искажения")
  void pricePrecisionIsPreservedOnCreateAndRead() throws Exception {
    JsonNode created =
        createOrder(
            orderJson("Alice", "[{\"productName\":\"Widget\",\"quantity\":1,\"price\":10.50}]"));
    String id = created.path("id").asText();

    assertThat(created.path("items").get(0).path("price").decimalValue())
        .isEqualByComparingTo(new BigDecimal("10.50"));

    MvcResult read = mockMvc.perform(get("/orders/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode readBody = JSON.readTree(read.getResponse().getContentAsString());
    assertThat(readBody.path("items").get(0).path("price").isNumber()).isTrue();
    assertThat(readBody.path("items").get(0).path("price").decimalValue())
        .isEqualByComparingTo(new BigDecimal("10.50"));
  }
}
