package io.synthesia.async;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.synthesia.async.dto.SignRequestMessage;
import io.synthesia.crypto.CryptoClient;
import java.util.Optional;
import java.util.concurrent.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class MessageSigningProcessorTest {

  @Mock private CryptoClient cryptoClient;

  @Mock private WebhookClient webhookClient;

  private BlockingQueue<SignRequestMessage> processorsQueue;

  @Mock private MessageSigningQueue messageSigningQueue;

  private MessageSingingProcessor sut;

  @BeforeEach
  public void beforeEach() {
    MockitoAnnotations.initMocks(this);

    this.processorsQueue = new LinkedBlockingQueue<>();

    this.sut =
        new MessageSingingProcessor(
            this.cryptoClient,
            this.webhookClient,
            this.processorsQueue,
            1,
            this.messageSigningQueue);
  }

  @Test
  void run_whenNoMessagesInProcessorQueue_doesNotAttemptToSign() {
    this.runSut();

    verify(this.cryptoClient, Mockito.times(0)).sign(any());
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignFails_doesNotCallTheWebhook() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.empty());

    this.runSut();

    verify(this.cryptoClient, Mockito.times(0)).sign(any());
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignThrows_doesNotCallTheWebhook() {
    when(this.cryptoClient.sign(any())).thenThrow(RuntimeException.class);

    this.runSut();

    verify(this.cryptoClient, Mockito.times(0)).sign(any());
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignSucceedsButWebhookThrows_doesNotAcknowledgeMessage() {
    var signRequestMessage = new SignRequestMessage("message", "webhook");
    signRequestMessage.setReceiptHandle("receipt");

    this.processorsQueue.add(signRequestMessage);

    when(this.cryptoClient.sign(any())).thenReturn(Optional.of("signedMessage"));

    when(this.webhookClient.notify(any(), any())).thenThrow(RuntimeException.class);

    this.runSut();

    verify(this.cryptoClient, Mockito.times(1)).sign(any());
    verify(this.messageSigningQueue, Mockito.times(0)).acknowledge(any());
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignSucceedsAndWebhookSucceeds_acknowledgesMessage() {
    var signRequestMessage = new SignRequestMessage("message", "webhook");
    signRequestMessage.setReceiptHandle("receipt");

    this.processorsQueue.add(signRequestMessage);

    when(this.cryptoClient.sign(any())).thenReturn(Optional.of("signedMessage"));

    when(this.webhookClient.notify(any(), any())).thenReturn(true);

    this.runSut();

    verify(this.cryptoClient, Mockito.times(1)).sign(any());
    verify(this.messageSigningQueue, Mockito.times(1)).acknowledge(any());
  }

  @SneakyThrows
  private void runSut() {
    ExecutorService carrier = Executors.newSingleThreadExecutor();

    carrier.execute(this.sut);

    Thread.sleep(1000);

    carrier.shutdown();

    carrier.awaitTermination(2, TimeUnit.SECONDS);

    carrier.shutdownNow();
  }
}
