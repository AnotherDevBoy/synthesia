package io.synthesia.async;

import io.synthesia.async.dto.SignRequestMessage;
import io.synthesia.crypto.CryptoClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MessageSingingProcessor implements Runnable {
  private final CryptoClient cryptoClient;
  private final HttpClient webhookClient;
  private final BlockingQueue<SignRequestMessage> processorsQueue;
  private final int producerQueueTimeoutInSeconds;
  private final MessageSigningQueue messageSigningQueue;

  @Override
  public void run() {
    try {
      log.info("MessageSingingProcessor started");

      while (true) {
        final SignRequestMessage signRequestMessage =
            this.processorsQueue.poll(this.producerQueueTimeoutInSeconds, TimeUnit.SECONDS);

        if (signRequestMessage == null) {
          continue;
        }

        if (signRequestMessage.getReceiptHandle() == null) {
          log.error("Missing receipt handle on message");
          continue;
        }

        var maybeSignedMessage = this.cryptoClient.sign(signRequestMessage.getMessage());

        if (maybeSignedMessage.isEmpty()) {
          continue;
        }

        var signedMessage = maybeSignedMessage.get();

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(
                    new URI(
                        String.format(
                            "%s?signedMessage=%s",
                            signRequestMessage.getWebhookUrl(), signedMessage)))
                .GET()
                .build();

        var response = this.webhookClient.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() >= 300) {
          continue;
        }

        this.messageSigningQueue.acknowledge(signRequestMessage);

        log.info("SignRequestMessage processed successfully");
      }
    } catch (final Exception e) {
      log.error("Encountered an error while processing SignRequestMessage", e);
    }
  }
}
