package com.howhow.functions.handler.http;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Context;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueStorageException;
import com.azure.storage.queue.models.SendMessageResult;
import com.azure.storage.queue.models.UpdateMessageResult;
import com.howhow.functions.model.dto.ErrorMessageDTO;
import com.howhow.functions.model.dto.MessageDTO;
import com.howhow.functions.model.dto.QueueMsgDTO;
import com.howhow.functions.utils.JsonUtils;
import com.howhow.functions.utils.QueueUtils;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class FinalDemoHttpHandler {

  @FunctionName("GetQueueMessage")
  public HttpResponseMessage getQueueMessage(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "queue/{queueName}/messages")
          HttpRequestMessage<String> request,
      @BindingName("queueName") String queueName,
      final ExecutionContext context) {

    Logger logger = context.getLogger();
    Map<String, String> reqParams = request.getQueryParameters();

    logger.info("Get Parameters : " + reqParams);

    String maxMsg = reqParams.getOrDefault("maxMsg", "1");
    String visibleTimeOut = reqParams.getOrDefault("visibleTimeOut", "30");
    Duration visibleTimeOutDuration = Duration.ofSeconds(Integer.valueOf(visibleTimeOut));
    QueueClient queueClient;
    try {
      queueClient = QueueUtils.createQueueClient(queueName, QueueUtils.getDefaultConnString());
    } catch (QueueStorageException e) {
      logger.warning("create queue client error:%s" + e.getMessage());

      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("content-type", "application/json")
          .body(createErrorResponseBody(e.getErrorCode().toString(), e.getServiceMessage()))
          .build();
    }

    PagedIterable<QueueMessageItem> queueMessageItems =
        queueClient.receiveMessages(
            Integer.valueOf(maxMsg), visibleTimeOutDuration, Duration.ofSeconds(30), Context.NONE);
    List<QueueMsgDTO> queueMsgDTOList;
    try {
      queueMsgDTOList =
          queueMessageItems.stream()
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
    } catch (QueueStorageException e) {

      logger.warning("query queue message error:%s" + e.getMessage());

      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("content-type", "application/json")
          .body(createErrorResponseBody(e.getErrorCode().toString(), e.getServiceMessage()))
          .build();
    }
    return request
        .createResponseBuilder(HttpStatus.OK)
        .header("content-type", "application/json")
        .body(JsonUtils.toJsonString(queueMsgDTOList))
        .build();
  }

  @FunctionName("addQueueMessage")
  public HttpResponseMessage addQueueMessage(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "queue/{queueName}/messages")
          HttpRequestMessage<MessageDTO> request,
      @BindingName("queueName") String queueName,
      final ExecutionContext context) {
    String message = request.getBody().getMessage();
    Logger logger = context.getLogger();
    // valid data
    if (StringUtils.isEmpty(message)) {
      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("content-type", "application/json")
          .body("message data is null")
          .build();
    }
    //  send Message
    QueueClient queueClient =
        QueueUtils.createQueueClient(queueName, QueueUtils.getDefaultConnString());
    SendMessageResult sendResult;
    try {
      sendResult = queueClient.sendMessage(message);
    } catch (QueueStorageException e) {
      logger.warning("send queue message error:%s" + e.getServiceMessage());

      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("content-type", "application/json")
          .body(createErrorResponseBody(e.getErrorCode().toString(), e.getServiceMessage()))
          .build();
    }

    QueueMsgDTO queueMsgDTO = new QueueMsgDTO();
    queueMsgDTO.setMessageId(sendResult.getMessageId());
    queueMsgDTO.setInsertionTime(sendResult.getInsertionTime());
    queueMsgDTO.setExpirationTime(sendResult.getExpirationTime());
    queueMsgDTO.setNextVisibleTime(sendResult.getTimeNextVisible());
    queueMsgDTO.setPopReceipt(sendResult.getPopReceipt());

    return request
        .createResponseBuilder(HttpStatus.OK)
        .header("content-type", "application/json")
        .body(JsonUtils.toJsonString(queueMsgDTO))
        .build();
  }

  @FunctionName("updateQueueMessage")
  public HttpResponseMessage updateQueueMessage(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.PUT},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "queue/{queueName}/messages/{messageId}")
          HttpRequestMessage<Optional<MessageDTO>> request,
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
    // update message with queue
    if (!request.getBody().isPresent()) {
      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("message body is required")
          .build();
    } else {
      QueueClient queueClient =
          QueueUtils.createQueueClient(queueName, QueueUtils.getDefaultConnString());
      UpdateMessageResult updateMessageResult =
          queueClient.updateMessage(
              messageId, popReceipt, request.getBody().get().getMessage(), Duration.ofSeconds(1));
      // create queue success response
      QueueMsgDTO queueMsgDTO = new QueueMsgDTO();
      queueMsgDTO.setNextVisibleTime(updateMessageResult.getTimeNextVisible());
      queueMsgDTO.setPopReceipt(updateMessageResult.getPopReceipt());

      return request
          .createResponseBuilder(HttpStatus.OK)
          .header("content-type", "application/json")
          .body(JsonUtils.toJsonString(queueMsgDTO))
          .build();
    }
  }

  @FunctionName("deleteQueueMessage")
  public HttpResponseMessage deleteQueueMessage(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.DELETE},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "queue/{queueName}/messages/{messageId}")
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
    try {
      queueClient.deleteMessage(messageId, popReceipt);
    } catch (QueueStorageException e) {
      logger.warning("delete queue message error:%s" + e.getServiceMessage());

      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body(createErrorResponseBody(e.getErrorCode().toString(), e.getServiceMessage()))
          .build();
    }

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setMessage(String.format("delete message %s success", messageId));
    return request
        .createResponseBuilder(HttpStatus.OK)
        .header("content-type", "application/json")
        .body(JsonUtils.toJsonString(messageDTO))
        .build();
  }

  private String createErrorResponseBody(String errorCode, String errorMessage) {
    ErrorMessageDTO errorMessageDTO = new ErrorMessageDTO();
    errorMessageDTO.setErrorCode(errorCode);
    errorMessageDTO.setErrorMessage(errorMessage);
    return JsonUtils.toJsonString(errorMessageDTO);
  }
}
