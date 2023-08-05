package io.synthesia.async.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public abstract class MessageBodyWithReceipt {
  /**
   * This is used to acknowledge the reception of the message so that it does not reappear in the
   * queue
   */
  @Setter private String receiptHandle;
}
