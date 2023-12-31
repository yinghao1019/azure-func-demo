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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // TODO check request body value
    String body = request.getBody();
    logger.info("Received Alert Body: " + body);

    //    parse slack api
    ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    JsonNode alertBodyJsonNode = objectMapper.readTree(body);
    JsonNode linkToSearchResults = alertBodyJsonNode.findValue("linkToSearchResultsAPI");
    if (linkToSearchResults != null) {
      String linkToSearchResultsAPI = linkToSearchResults.asText();
      // call slack api
      String alertMessage = "";
      try {
        alertMessage = getAlertMessage(linkToSearchResultsAPI, logger);
      } catch (Exception e) {
        logger.warning("parse alert message error: " + e.getMessage());
      }

      logger.info("after process alert message" + alertMessage);
      try {
        ObjectNode slackMessage = objectMapper.createObjectNode();
        slackMessage.set("text", new TextNode(alertMessage));
        String webHookResponse =
            HttpClientUtils.postJsonRequest(webhookUrl, slackMessage.toString());
        logger.info(webHookResponse);
      } catch (IOException e) {
        logger.warning("slack webhook error: " + e.getMessage());
      }
    }

    return request.createResponseBuilder(HttpStatus.OK).body("Received alert").build();
  }

  private String getAlertMessage(String linkToSearchResultsAPI, Logger logger) throws IOException {
    logger.info("application api :" + linkToSearchResultsAPI);

    String searchResult = getSearchResults(linkToSearchResultsAPI);

    StringBuilder stringBuilder = new StringBuilder();

    ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    JsonNode searchResultJsonNode = objectMapper.readTree(searchResult);
    // parse common alert webhook schema
    JsonNode columnsNode = searchResultJsonNode.findValue("columns");

    if (columnsNode.isArray()) {
      List<String> columnNameList = new ArrayList<>();
      for (JsonNode objNode : columnsNode) {
        columnNameList.add(objNode.get("name").asText());
      }
      stringBuilder.append(String.join(", ", columnNameList));
      stringBuilder.append("\n");
    }

    // parse row
    JsonNode rowsNode = searchResultJsonNode.findValue("rows");
    if (rowsNode.isArray()) {
      for (JsonNode rowNode : rowsNode) {
        ArrayNode arrayNode = (ArrayNode) rowNode;
        List<String> valueList = new ArrayList<>();
        for (JsonNode valueNode : arrayNode) {
          if (valueNode.isValueNode()) {
            valueList.add(valueNode.asText());
          }
        }
        stringBuilder.append(String.join(", ", valueList) + "\n");
      }
    }
    return stringBuilder.toString();
  }

  private String getSearchResults(String linkToSearchResultsApi) throws IOException {
    String applicationInsightKey = System.getenv("APPLICATION_INSIGHTS_KEY");

    Map<String, String> header = new HashMap<>();
    header.put("x-api-key", applicationInsightKey);
    return HttpClientUtils.getRequest(linkToSearchResultsApi, header);
  }
}
