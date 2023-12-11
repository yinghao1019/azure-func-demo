package com.howhow.functions.model.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueMsgDTO {
  private String messageId;
  private String body;
  private String popReceipt;
  private Long dequeueCount;
  private OffsetDateTime nextVisibleTime;
  private OffsetDateTime expirationTime;
  private OffsetDateTime insertionTime;
}
