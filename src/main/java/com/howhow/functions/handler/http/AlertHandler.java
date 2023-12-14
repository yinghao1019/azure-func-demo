package com.howhow.functions.handler.http;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.logging.Logger;

public class AlertHandler {

  @FunctionName("HandleExceptions")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "exceptions")
          HttpRequestMessage<String> request,
      final ExecutionContext context) {

    Logger logger = context.getLogger();
    String body = request.getBody();
    logger.info("Received Alert Body: " + body);
    return request
        .createResponseBuilder(HttpStatus.OK)
        .header("content-type", "application/json")
        .body(body)
        .build();
  }
}
