package io.synthesia.api;

import com.google.inject.Inject;
import io.javalin.http.Context;
import io.synthesia.api.dto.SignRequestDTO;
import io.synthesia.async.MessageSigningQueue;
import io.synthesia.crypto.CryptoClient;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__(@Inject))
public class SignApi {
  private CryptoClient cryptoClient;
  private MessageSigningQueue messageSigningQueue;

  public void sign(Context context) {
    SignRequestDTO signRequestDTO = context.bodyAsClass(SignRequestDTO.class);
    // TODO: return 400 if invalid SignRequest (null or empty message, null or empty webhook)

    var maybeSignature = cryptoClient.sign(signRequestDTO.getMessage());

    if (maybeSignature.isPresent()) {
      context.result(maybeSignature.get());
      return;
    }

    this.messageSigningQueue.scheduleMessageSigning(signRequestDTO.toSignRequestMessage());
    context.status(202);
  }
}
