package io.synthesia.api;

import com.google.inject.Inject;
import io.javalin.http.Context;
import io.synthesia.api.dto.SignRequestDTO;
import io.synthesia.async.MessageSigningQueue;
import io.synthesia.crypto.CryptoClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor(onConstructor = @__(@Inject))
public class SignApi {
  private CryptoClient cryptoClient;
  private MessageSigningQueue messageSigningQueue;

  public void sign(Context context) {
    log.info("Received Sign request");
    SignRequestDTO signRequestDTO = context.bodyAsClass(SignRequestDTO.class);

    if (!isValidRequestDTO(signRequestDTO)) {
      context.status(400);
      return;
    }

    var maybeSignature = cryptoClient.sign(signRequestDTO.getMessage());

    if (maybeSignature.isPresent()) {
      context.result(maybeSignature.get());
      log.info("Sign request processed synchronously");
      return;
    }

    this.messageSigningQueue.scheduleMessageSigning(signRequestDTO.toSignRequestMessage());
    context.status(202);
    log.info("Sign request processed asynchronously");
  }

  // TODO: validate webhook is a valid URL
  private boolean isValidRequestDTO(SignRequestDTO signRequestDTO) {
    return signRequestDTO != null
        && signRequestDTO.getMessage() != null
        && !signRequestDTO.getMessage().isBlank()
        && signRequestDTO.getWebhookUrl() != null
        && !signRequestDTO.getWebhookUrl().isBlank();
  }
}
