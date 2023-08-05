package io.synthesia.async;

import io.synthesia.async.dto.SignRequestMessage;
import java.util.List;

public interface MessageSigningQueue {
  void scheduleMessageSigning(SignRequestMessage signRequestMessage);

  List<SignRequestMessage> getMessagesToSign();

  void acknowledge(SignRequestMessage signRequestMessage);
}
