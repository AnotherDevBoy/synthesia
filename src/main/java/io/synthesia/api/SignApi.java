package io.synthesia.api;

import io.javalin.http.Context;
import io.synthesia.api.dto.SignRequestDTO;
import io.synthesia.crypto.CryptoClient;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SignApi {
  private CryptoClient cryptoClient;

  public void sign(Context context) {
    SignRequestDTO signRequestDTO = context.bodyAsClass(SignRequestDTO.class);

    var maybeSignature = cryptoClient.sign(signRequestDTO.getMessage());

    if (maybeSignature.isPresent()) {
      context.result(maybeSignature.get());
      return;
    }

    context.status(202);
  }
}
