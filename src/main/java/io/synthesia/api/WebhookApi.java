package io.synthesia.api;

import com.google.inject.Inject;
import io.javalin.http.Context;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor(onConstructor = @__(@Inject))
public class WebhookApi {
  public void notify(Context context) {
    var signedMessage = context.queryParam("signedMessage");
    log.info("Webhook notification received with message {}", signedMessage);
    context.result(signedMessage);
  }
}
