package com.howhow.functions.handler.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.howhow.functions.utils.HttpClientUtils;
import com.howhow.functions.utils.JsonUtils;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AlertHandler {

  private static final String SLACK_WEBHOOK_URL = "SlackExceptionsWebHook";

  @FunctionName("HandleExceptions")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.ANONYMOUS,
              route = "exceptions")
          HttpRequestMessage<String> request,
      final ExecutionContext context)
      throws JsonProcessingException {

    Logger logger = context.getLogger();
    String webhookUrl = System.getenv(SLACK_WEBHOOK_URL);
    logger.info("WebHookUrl" + webhookUrl);

    // TODO check request body value
    String body = request.getBody();
    logger.info("Received Alert Body: " + body);

    //    parse slack api
    ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    JsonNode alertBodyJsonNode = objectMapper.readTree(body);
    String linkToSearchResultsAPI = alertBodyJsonNode.findValue("linkToSearchResultsAPI").asText();

    // call slack api
    String alertMessage = "";
    try {
      alertMessage = getAlertMessage(linkToSearchResultsAPI, logger);
    } catch (Exception e) {
      logger.warning("parse alert message error " + e.getMessage());
    }

    logger.info("alert message" + alertMessage);
    try {
      ObjectNode slackMessage = objectMapper.createObjectNode();
      slackMessage.set("text", new TextNode(alertMessage));
      String webHookResponse = HttpClientUtils.postJsonRequest(webhookUrl, slackMessage.toString());
      logger.info(webHookResponse);
    } catch (IOException e) {
      logger.warning("slack webhook error" + e.getMessage());
    }
    return request.createResponseBuilder(HttpStatus.OK).body("Received alert").build();
  }

  private String getAlertMessage(String linkToSearchResultsAPI, Logger logger) throws IOException {
    String searchResult = getSearchResults(linkToSearchResultsAPI, logger);
    logger.info("SearchResult : " + searchResult);
    StringBuilder stringBuilder = new StringBuilder();

    ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    JsonNode searchResultJsonNode = objectMapper.readTree(searchResult);
    // parse common alert webhook schema
    JsonNode columnsNode = searchResultJsonNode.findValue("columns");
    logger.info(columnsNode.toString());
    if (columnsNode.isArray()) {
      for (JsonNode objNode : columnsNode) {
        String columnName = objNode.get("name").asText();
        stringBuilder.append(columnName + " ");
      }
      stringBuilder.append("\n");
    }
    JsonNode rowsNode = searchResultJsonNode.findValue("rows");
    logger.info(rowsNode.toString());
    // parse row
    if (rowsNode.isArray()) {
      for (JsonNode rowNode : rowsNode) {
        ArrayNode arrayNode = (ArrayNode) rowNode;
        for (JsonNode valueNode : arrayNode) {
          if (valueNode.isValueNode()) {
            stringBuilder.append(valueNode.asText() + " ");
          }
        }
        stringBuilder.append("\n");
      }
    }
    return stringBuilder.toString();
  }

  private String getSearchResults(String linkToSearchResultsApi, Logger logger) throws IOException {
    String applicationInsightKey = System.getenv("APPLICATION_INSIGHTS_KEY");

    logger.info("key" + applicationInsightKey);
    Map<String, String> header = new HashMap<>();
    header.put("x-api-key", applicationInsightKey);
    return HttpClientUtils.getRequest(linkToSearchResultsApi, header);
  }
}
