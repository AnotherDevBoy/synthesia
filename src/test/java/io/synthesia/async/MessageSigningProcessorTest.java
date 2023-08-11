package io.synthesia.async;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.synthesia.async.dto.SignRequestMessage;
import io.synthesia.crypto.CryptoClient;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

    assertNoMessageSigningAttempted();
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignFails_doesNotCallTheWebhook() {
    givenSignRequestMessageToProcess();

    whenSigningFails();

    this.runSut();

    assertWebhookNotCalled();
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignThrows_doesNotCallTheWebhook() {
    givenSignRequestMessageToProcess();

    whenSigningThrows();

    this.runSut();

    assertWebhookNotCalled();
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignSucceedsButWebhookThrows_doesNotAcknowledgeMessage() {
    givenSignRequestMessageToProcess();

    whenSigningSucceeds();

    whenWebhookThrows();

    this.runSut();

    assertMessageSigningRequestNotAcknowledged();
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignSucceedsButWebhookFails_doesNotAcknowledgeMessage() {
    givenSignRequestMessageToProcess();

    whenSigningSucceeds();

    whenWebhookFails();

    this.runSut();

    assertMessageSigningRequestNotAcknowledged();
  }

  @Test
  void run_whenMessagesInProcessorQueueAndSignSucceedsAndWebhookSucceeds_acknowledgesMessage() {
    givenSignRequestMessageToProcess();

    whenSigningSucceeds();

    whenWebhookSucceeds();

    this.runSut();

    assertMessageSigningRequestAcknowledged();
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

  private void givenSignRequestMessageToProcess() {
    var signRequestMessage = new SignRequestMessage("message", "webhook");
    signRequestMessage.setReceiptHandle("receipt");

    this.processorsQueue.add(signRequestMessage);
  }

  private void whenSigningFails() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.empty());
  }

  private void whenSigningThrows() {
    when(this.cryptoClient.sign(any())).thenThrow(RuntimeException.class);
  }

  private void whenSigningSucceeds() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.of("signedMessage"));
  }

  private void whenWebhookFails() {
    when(this.webhookClient.notify(any(), any())).thenReturn(false);
  }

  private void whenWebhookThrows() {
    when(this.webhookClient.notify(any(), any())).thenThrow(RuntimeException.class);
  }

  private void whenWebhookSucceeds() {
    when(this.webhookClient.notify(any(), any())).thenReturn(true);
  }

  private void assertWebhookNotCalled() {
    verify(this.webhookClient, Mockito.times(0)).notify(any(), any());
  }

  private void assertNoMessageSigningAttempted() {
    verify(this.cryptoClient, Mockito.times(0)).sign(any());
  }

  private void assertMessageSigningRequestNotAcknowledged() {
    verify(this.messageSigningQueue, Mockito.times(0)).acknowledge(any());
  }

  private void assertMessageSigningRequestAcknowledged() {
    verify(this.messageSigningQueue, Mockito.times(1)).acknowledge(any());
  }
}
