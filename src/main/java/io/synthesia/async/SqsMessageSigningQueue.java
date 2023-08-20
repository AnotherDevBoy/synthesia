package io.synthesia.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synthesia.async.dto.SignRequestMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@AllArgsConstructor
public class SqsMessageSigningQueue implements MessageSigningQueue {
  private final SqsClient client;
  private final String queueUrl;
  private final int waitTimeInSeconds;
  private final int visibilityTimeout;
  private final ObjectMapper mapper;

  @SneakyThrows
  @Override
  public void scheduleMessageSigning(SignRequestMessage signRequestMessage) {
    final String messageBody = this.mapper.writeValueAsString(signRequestMessage);

    log.trace("Sending message with body {}", messageBody);

    this.client.sendMessage(
        SendMessageRequest.builder().messageBody(messageBody).queueUrl(this.queueUrl).build());
  }

  @Override
  public List<SignRequestMessage> getMessagesToSign() {
    log.trace("Receive messages from {}", this.queueUrl);
    final ReceiveMessageResponse response =
        this.client.receiveMessage(
            ReceiveMessageRequest.builder()
                .waitTimeSeconds(this.waitTimeInSeconds)
                .visibilityTimeout(this.visibilityTimeout)
                .queueUrl(this.queueUrl)
                .maxNumberOfMessages(10)
                .build());

    final List<SignRequestMessage> messages = new ArrayList<>();

    response
        .messages()
        .forEach(
            sqsMessage -> {
              try {
                log.trace("Received message with body {}", sqsMessage.body());
                final SignRequestMessage message =
                    this.mapper.readValue(sqsMessage.body(), SignRequestMessage.class);

                if (message != null) {
                  message.setReceiptHandle(sqsMessage.receiptHandle());
                  messages.add(message);
                }
              } catch (final Exception e) {
                log.error("Couldn't deserialize message", e);
              }
            });

    return messages;
  }

  @Override
  public void acknowledge(SignRequestMessage signRequestMessage) {
    log.debug(
        "Delete message from {} with receipt {}",
        this.queueUrl,
        signRequestMessage.getReceiptHandle());
    this.client.deleteMessage(
        DeleteMessageRequest.builder()
            .queueUrl(this.queueUrl)
            .receiptHandle(signRequestMessage.getReceiptHandle())
            .build());
  }
}
