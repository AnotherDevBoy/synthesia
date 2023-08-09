package io.synthesia.async;

public interface WebhookClient {
  boolean notify(String webhookUrl, String signedMessage);
}
