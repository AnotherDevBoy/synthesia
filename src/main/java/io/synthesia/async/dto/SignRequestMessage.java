package io.synthesia.async.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SignRequestMessage extends MessageBodyWithReceipt {
  private String message;
  private String webhookUrl;
}
