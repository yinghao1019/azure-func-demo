package com.howhow.functions.handler.queue;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;
import java.util.logging.Logger;

public class FinalDemoQueueHandler {
  @StorageAccount("AzureWebJobsStorage")
  @FunctionName("AsyncGetQueueMsg")
  public void asyncGetQueueMsg(
      @BindingName("Id") String messageId,
      @QueueTrigger(name = "message", queueName = "final-demo") String message,
      final ExecutionContext context) {
    Logger logger = context.getLogger();
    logger.info("receive Message. id: " + messageId + ", message: " + message);
  }
}
