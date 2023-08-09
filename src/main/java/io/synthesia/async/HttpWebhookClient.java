package io.synthesia.async;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class HttpWebhookClient implements WebhookClient {
  private final HttpClient webhookClient;

  @SneakyThrows
  @Override
  public boolean notify(String webhookUrl, String signedMessage) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(new URI(String.format("%s?signedMessage=%s", webhookUrl, signedMessage)))
            .GET()
            .build();

    var response = this.webhookClient.send(request, HttpResponse.BodyHandlers.discarding());

    return response.statusCode() < 300;
  }
}
