package io.synthesia.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.synthesia.async.MessageSigningQueue;
import io.synthesia.async.SqsMessageSigningQueue;
import java.net.URI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

@Slf4j
public class QueueModule extends AbstractModule {
  private static final String QUEUE_NAME = "sign-queue";

  @SneakyThrows
  @Provides
  @Singleton
  public MessageSigningQueue messageSigningQueueProvider() {
    final var clientBuilder =
        SqsClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.US_EAST_1)
            .endpointOverride(new URI("http://localhost:4566"));

    final var client = clientBuilder.build();

    final String queueUrl = this.createSqsQueue(client);

    return new SqsMessageSigningQueue(client, queueUrl, 10, 20, new ObjectMapper());
  }

  @SneakyThrows
  private String createSqsQueue(final SqsClient client) {
    log.debug("Create queue with name {}", QUEUE_NAME);
    final CreateQueueResponse createQueueResponse =
        client.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build());

    final String createdQueueUrl = createQueueResponse.queueUrl();
    log.debug("Created queue {}", createdQueueUrl);

    return createdQueueUrl;
  }
}
