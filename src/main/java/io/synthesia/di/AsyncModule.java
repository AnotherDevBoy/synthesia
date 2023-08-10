package io.synthesia.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.synthesia.async.*;
import io.synthesia.async.dto.SignRequestMessage;
import io.synthesia.crypto.CryptoClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class AsyncModule extends AbstractModule {
  @Provides
  @Singleton
  public BlockingQueue<SignRequestMessage> blockingQueueProvider() {
    // TODO: Make this configurable
    return new LinkedBlockingDeque<>(100);
  }

  @Provides
  @Singleton
  public WebhookClient webhookClientProvider() {
    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();

    return new HttpWebhookClient(httpClient);
  }

  @Provides
  @Singleton
  public MessageSigningConsumer messageSigningConsumerProvider(
      MessageSigningQueue messageSigningQueue, BlockingQueue<SignRequestMessage> consumerQueue) {
    return new MessageSigningConsumer(messageSigningQueue, consumerQueue);
  }

  @Provides
  @Singleton
  public MessageSingingProcessor messageSingingProcessorProvider(
      CryptoClient cryptoClient,
      WebhookClient webhookClient,
      BlockingQueue<SignRequestMessage> producerQueue,
      MessageSigningQueue messageSigningQueue) {

    return new MessageSingingProcessor(
        cryptoClient, webhookClient, producerQueue, 10, messageSigningQueue);
  }
}