package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class OrderReplaceApiTest extends AbstractOrdersApiTest {

  private static final String NEW_ITEM =
      "{\"productName\":\"Gadget\",\"quantity\":5,\"price\":99.99}";

  private MvcResult putRaw(String id, String body) throws Exception {
    return mockMvc
        .perform(put("/orders/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn();
  }

  @Test
  @DisplayName("Полная замена меняет имя, позиции и статус; id и createdAt остаются прежними")
  void replaceChangesMutableFieldsKeepsIdentity() throws Exception {
    JsonNode created = createOrder(validOrderJson());
    String id = created.path("id").asText();

    MvcResult result =
        mockMvc
            .perform(
                put("/orders/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderJson("Bob", "[" + NEW_ITEM + "]", "CONFIRMED")))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
    assertThat(body.path("id").asText()).isEqualTo(id);
    assertThat(body.path("createdAt").asText()).isEqualTo(created.path("createdAt").asText());
    assertThat(body.path("customerName").asText()).isEqualTo("Bob");
    assertThat(body.path("status").asText()).isEqualTo("CONFIRMED");
    assertThat(body.path("items")).hasSize(1);
    assertThat(body.path("items").get(0).path("productName").asText()).isEqualTo("Gadget");
    assertThat(body.path("items").get(0).path("quantity").asInt()).isEqualTo(5);
    assertThat(body.path("items").get(0).path("price").decimalValue())
        .isEqualByComparingTo("99.99");

    MvcResult read = mockMvc.perform(get("/orders/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode readBody = JSON.readTree(read.getResponse().getContentAsString());
    assertThat(readBody.path("customerName").asText()).isEqualTo("Bob");
    assertThat(readBody.path("status").asText()).isEqualTo("CONFIRMED");
  }

  @Test
  @DisplayName("id из тела PUT игнорируется: меняется заказ из пути, другой id не появляется")
  void replaceIgnoresIdFromBody() throws Exception {
    JsonNode created = createOrder(validOrderJson());
    String id = created.path("id").asText();
    String otherId = UUID.randomUUID().toString();
    String body =
        "{\"id\":\""
            + otherId
            + "\",\"customerName\":\"Bob\",\"items\":["
            + NEW_ITEM
            + "],\"status\":\"CONFIRMED\"}";

    MvcResult result = mockMvc
        .perform(put("/orders/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode response = JSON.readTree(result.getResponse().getContentAsString());
    assertThat(response.path("id").asText()).isEqualTo(id);
    assertThat(response.path("customerName").asText()).isEqualTo("Bob");
    mockMvc.perform(get("/orders/" + otherId)).andExpect(status().isNotFound());
    assertThat(listOrders()).hasSize(1);
  }

  @Test
  @DisplayName("PUT несуществующего заказа даёт 404 и не создаёт заказ")
  void replaceUnknownOrderReturnsNotFoundAndDoesNotCreate() throws Exception {
    mockMvc
        .perform(
            put("/orders/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
        .andExpect(status().isNotFound());

    assertNoOrdersExist();
  }

  @ParameterizedTest(name = "[{index}] тело: {0}")
  @ValueSource(
      strings = {
        "",
        "{broken json",
        "{}",
        "{\"customerName\":\"Bob\"}",
        "{\"items\":[{\"productName\":\"Gadget\",\"quantity\":5,\"price\":99.99}]}",
        "{\"customerName\":\"\",\"items\":[{\"productName\":\"Gadget\",\"quantity\":5,\"price\":99.99}]}",
        "{\"customerName\":\"Bob\",\"items\":[]}",
        "{\"customerName\":\"Bob\",\"items\":[{\"productName\":\"Gadget\",\"quantity\":0,\"price\":99.99}]}",
        "{\"customerName\":\"Bob\",\"items\":[{\"productName\":\"Gadget\",\"quantity\":5,\"price\":-1}]}",
        "{\"customerName\":\"Bob\",\"items\":[{\"productName\":\"Gadget\",\"quantity\":5,\"price\":99.99}],\"status\":\"SHIPPED\"}"
      })
  @DisplayName("PUT с пустым, битым, неполным или нарушающим правила телом даёт 400, заказ не меняется")
  void replaceWithInvalidBodyReturnsBadRequestAndKeepsOrder(String body) throws Exception {
    JsonNode created = createOrder(validOrderJson());
    String id = created.path("id").asText();

    mockMvc
        .perform(put("/orders/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());

    MvcResult read = mockMvc.perform(get("/orders/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode after = JSON.readTree(read.getResponse().getContentAsString());
    assertThat(after).isEqualTo(created);
  }

  @Test
  @DisplayName("PUT с идентификатором в пути, не являющимся UUID, даёт 400, а не 404")
  void replaceWithNonUuidIdReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            put("/orders/not-a-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
        .andExpect(status().isBadRequest());
  }
}
