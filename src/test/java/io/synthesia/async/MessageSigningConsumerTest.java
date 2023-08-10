package io.synthesia.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.synthesia.async.dto.SignRequestMessage;
import java.util.List;
import java.util.concurrent.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MessageSigningConsumerTest {
  @Mock private MessageSigningQueue messageSigningQueue;

  private BlockingQueue<SignRequestMessage> processorsQueue;

  private MessageSigningConsumer sut;

  @BeforeEach
  public void beforeEach() {
    MockitoAnnotations.initMocks(this);

    this.processorsQueue = new LinkedBlockingQueue<>();

    this.sut = new MessageSigningConsumer(this.messageSigningQueue, this.processorsQueue);
  }

  @SneakyThrows
  @Test
  void run_whenNoMessagesInSigningQueue_doesNotAddMessagesToProcessorsQueue() {
    whenMessageSigningQueueIsEmpty();

    this.runSut();

    assertNoSignRequestAddedToProcessorsQueue();
  }

  @Test
  void run_whenMessagesInSigningQueue_addsMessagesToProcessorsQueue() {
    var message = new SignRequestMessage("message", "webhook");
    whenMessageSigningQueueHasMessage(message);

    this.runSut();

    assertSignRequestInProcessorsQueue(message);
  }

  @Test
  void run_readingFromSigningQueueThrows_itWillTryAgain() {
    whenMessageSigningQueueThrows();

    this.runSut();

    assertMessageSigningConsumerRetriesReadingFromQueue();
  }

  private void whenMessageSigningQueueIsEmpty() {
    when(this.messageSigningQueue.getMessagesToSign()).thenReturn(List.of());
  }

  private void whenMessageSigningQueueHasMessage(SignRequestMessage message) {
    when(this.messageSigningQueue.getMessagesToSign()).thenReturn(List.of(message));
  }

  private void whenMessageSigningQueueThrows() {
    when(this.messageSigningQueue.getMessagesToSign()).thenThrow(RuntimeException.class);
  }

  private void assertMessageSigningConsumerRetriesReadingFromQueue() {
    verify(this.messageSigningQueue, atLeast(2)).getMessagesToSign();
  }

  private void assertNoSignRequestAddedToProcessorsQueue() {
    assertEquals(0, this.processorsQueue.size());
  }

  @SneakyThrows
  private void assertSignRequestInProcessorsQueue(SignRequestMessage expectedMessage) {
    var actualMessage = this.processorsQueue.poll(1, TimeUnit.SECONDS);

    assertEquals(expectedMessage, actualMessage);
  }

  @SneakyThrows
  private void runSut() {
    ExecutorService carrier = Executors.newSingleThreadExecutor();

    carrier.execute(this.sut);

    Thread.sleep(1000);

    carrier.shutdown();

    carrier.awaitTermination(1, TimeUnit.SECONDS);

    carrier.shutdownNow();
  }
}
