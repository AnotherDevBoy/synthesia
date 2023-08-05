package io.synthesia.api.dto;

import io.synthesia.async.dto.SignRequestMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignRequestDTO {
  private String message;
  private String webhookUrl;

  public SignRequestMessage toSignRequestMessage() {
    return new SignRequestMessage(this.message, this.webhookUrl);
  }
}
