package io.synthesia.async;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synthesia.async.dto.SignRequestMessage;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Testcontainers
public class SqsMessageSigningQueueTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Container
  private static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.2"))
          .withServices(Service.SQS);

  private SqsClient sqsClient;

  private String queueUrl;

  private SqsMessageSigningQueue sut;

  @BeforeEach
  void beforeEach() {
    this.sqsClient =
        SqsClient.builder()
            .endpointOverride(localstack.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .region(Region.of(localstack.getRegion()))
            .build();

    this.queueUrl = this.createQueue();

    this.sut = new SqsMessageSigningQueue(this.sqsClient, queueUrl, 10, 20, OBJECT_MAPPER);
  }

  @Test
  void scheduleMessageSigning_addsMessageToQueue() {
    var signRequestMessage = new SignRequestMessage("message", "webhookUrl");
    this.sut.scheduleMessageSigning(signRequestMessage);

    var messages = this.sut.getMessagesToSign();
    assertEquals(1, messages.size());

    var actualMessage = messages.get(0);
    assertEquals(signRequestMessage, actualMessage);
  }

  @Test
  void getMessagesToSign_readsAtMost10messages() {
    var signRequestMessage = new SignRequestMessage("message", "webhookUrl");

    IntStream.range(0, 20).forEach(i -> this.sut.scheduleMessageSigning(signRequestMessage));

    var messages = this.sut.getMessagesToSign();
    assertEquals(10, messages.size());

    messages.forEach(actualMessage -> assertEquals(signRequestMessage, actualMessage));
  }

  @Test
  void getMessagesToSign_whenNoMessagesAvailable_returnsNoMessages() {
    var messages = this.sut.getMessagesToSign();
    assertEquals(0, messages.size());
  }

  @Test
  void getMessagesToSign_whenMessageCannotBeDeserialized_returnsNoMessages() {
    addInvalidMessage();
    var messages = this.sut.getMessagesToSign();
    assertEquals(0, messages.size());
  }

  @Test
  void acknowledge_deletesMessageFromQueue() {
    var signRequestMessage = new SignRequestMessage("message", "webhookUrl");
    this.sut.scheduleMessageSigning(signRequestMessage);

    var messages = this.sut.getMessagesToSign();

    var actualMessage = messages.get(0);

    this.sut.acknowledge(actualMessage);

    var queueAttributes = this.getQueueAttributes();

    assertEquals(0, queueAttributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
    assertEquals(
        0, queueAttributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
  }

  private String createQueue() {
    var request = CreateQueueRequest.builder().queueName("queue-" + UUID.randomUUID()).build();
    var response = this.sqsClient.createQueue(request);

    return response.queueUrl();
  }

  @SneakyThrows
  private void addInvalidMessage() {
    final String messageBody = OBJECT_MAPPER.writeValueAsString("asdasdasd");

    this.sqsClient.sendMessage(
        SendMessageRequest.builder().messageBody(messageBody).queueUrl(this.queueUrl).build());
  }

  private Map<QueueAttributeName, Integer> getQueueAttributes() {
    var request =
        GetQueueAttributesRequest.builder()
            .attributeNames(
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)
            .queueUrl(this.queueUrl)
            .build();
    var response = this.sqsClient.getQueueAttributes(request);

    return response.attributes().entrySet().stream()
        .collect(
            Collectors.toMap(entry -> entry.getKey(), entry -> Integer.parseInt(entry.getValue())));
  }
}
