package io.synthesia.async;

import io.synthesia.async.dto.SignRequestMessage;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MessageSigningConsumer implements Runnable {
  private final MessageSigningQueue messageSigningQueue;
  private final BlockingQueue<SignRequestMessage> processorsQueue;

  @Override
  public void run() {
    log.info("MessageSigningConsumer started");

    while (!Thread.currentThread().isInterrupted()) {
      try {
        final List<SignRequestMessage> messages = this.messageSigningQueue.getMessagesToSign();

        for (final SignRequestMessage message : messages) {
          this.processorsQueue.put(message);
        }
      } catch (final Exception e) {
        log.error("Encountered an error while pulling from queue", e);
      }
    }
  }
}
