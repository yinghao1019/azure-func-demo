package com.howhow.functions.handler.http;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.howhow.functions.utils.HttpClientUtils;
import com.howhow.functions.utils.JsonUtils;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.io.IOException;
import java.util.logging.Logger;

public class AlertHandler {

  private static final String SLACK_WEBHOOK_URL = "SlackExceptionsWebHook";

  @FunctionName("HandleExceptions")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "exceptions")
          HttpRequestMessage<String> request,
      final ExecutionContext context) {

    Logger logger = context.getLogger();
    String webhookUrl = System.getenv(SLACK_WEBHOOK_URL);
    logger.info("WebHookUrl" + webhookUrl);

    // TODO check request body value
    String body = request.getBody();
    logger.info("Received Alert Body: " + body);
    // call slack api
    ObjectNode slackMessage = JsonUtils.getObjectMapper().createObjectNode();
    slackMessage.set("text", new TextNode(body));
    try {
      String webHookResponse = HttpClientUtils.postJsonRequest(webhookUrl, slackMessage.toString());
      logger.info(webHookResponse);
    } catch (IOException e) {
      logger.warning("slack webhook error" + e.getMessage());
    }

    return request.createResponseBuilder(HttpStatus.OK).body("success received alert").build();
  }
}
