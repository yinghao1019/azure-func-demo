package com.howhow.functions.handler;

import com.azure.storage.queue.QueueClient;
import com.howhow.functions.utils.QueueUtils;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Map;
import java.util.logging.Logger;

public class QueueDemoHandler {

  @FunctionName("GetQueueMessage")
  public HttpResponseMessage getQueueMessage(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.ANONYMOUS,
              route = "/queue/{queueName:alpha}")
          HttpRequestMessage<String> request,
      @BindingName("queueName") String queueName,
      final ExecutionContext context) {
    Logger logger = context.getLogger();
    Map<String, String> reqParams = request.getQueryParameters();
    logger.info("Get Parameters : " + reqParams);

    String maxMsgCount = reqParams.getOrDefault("maxMsgCount", "1");
    String visibleTimeOut = reqParams.get("visibleTimeOut");
    // TODO connection string
    QueueClient queueClient = QueueUtils.createQueueClient(queueName, "");
    // TODO RECEIVE MESSAGE
    return request.createResponseBuilder(HttpStatus.OK).body("").build();
  }
}
