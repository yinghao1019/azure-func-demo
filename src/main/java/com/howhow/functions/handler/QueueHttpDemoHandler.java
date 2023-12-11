package com.howhow.functions.handler;

import com.azure.core.util.Context;
import com.azure.storage.queue.QueueClient;
import com.howhow.functions.model.dto.MessageDTO;
import com.howhow.functions.model.dto.QueueMsgDTO;
import com.howhow.functions.utils.JsonUtils;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class QueueHttpDemoHandler {

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

    String maxMsg = reqParams.getOrDefault("maxMsg", "1");
    String visibleTimeOut = reqParams.get("visibleTimeOut");
    Duration visibleTimeOutDuration = Duration.ofSeconds(Integer.valueOf(visibleTimeOut));
    QueueClient queueClient =
        QueueUtils.createQueueClient(queueName, QueueUtils.getDefaultConnString());

    List<QueueMsgDTO> queueMsgDTOList =
        queueClient
            .receiveMessages(
                Integer.valueOf(maxMsg),
                visibleTimeOutDuration,
                Duration.ofSeconds(30),
                Context.NONE)
            .stream()
            .map(
                queueMessageItem -> {
                  QueueMsgDTO queueMsgDTO = new QueueMsgDTO();
                  queueMsgDTO.setMessageId(queueMessageItem.getMessageId());
                  queueMsgDTO.setPopReceipt(queueMessageItem.getPopReceipt());
                  queueMsgDTO.setBody(queueMessageItem.getMessageText());
                  queueMsgDTO.setInsertionTime(queueMessageItem.getInsertionTime());
                  queueMsgDTO.setExpirationTime(queueMessageItem.getExpirationTime());
                  queueMsgDTO.setDequeueCount(queueMessageItem.getDequeueCount());
                  return queueMsgDTO;
                })
            .collect(Collectors.toList());

    return request
        .createResponseBuilder(HttpStatus.OK)
        .body(JsonUtils.toJsonString(queueMsgDTOList))
        .build();
  }

  @FunctionName("deleteQueueMessage")
  public HttpResponseMessage deleteQueueMessage(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.DELETE},
              authLevel = AuthorizationLevel.ANONYMOUS,
              route = "/queue/{queueName:alpha}/messages/{messageId:aplha}")
          HttpRequestMessage<String> request,
      @BindingName("queueName") String queueName,
      @BindingName("messageId") String messageId,
      final ExecutionContext context) {

    Logger logger = context.getLogger();
    Map<String, String> reqParams = request.getQueryParameters();

    logger.info("Get Parameters : " + reqParams);
    // check parameters
    String popReceipt = reqParams.get("popReceipt");
    if (popReceipt == null) {
      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("popReceipt is required")
          .build();
    }
    // delete message with queue
    QueueClient queueClient =
        QueueUtils.createQueueClient(queueName, QueueUtils.getDefaultConnString());

    queueClient.deleteMessage(messageId, popReceipt);
    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setMessage(String.format("delete message %s success", messageId));
    return request
        .createResponseBuilder(HttpStatus.OK)
        .body(JsonUtils.toJsonString(messageDTO))
        .build();
  }
}
