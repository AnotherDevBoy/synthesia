package io.synthesia.async.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignRequestMessage extends MessageBodyWithReceipt {
  private String message;
  private String webhookUrl;
}
