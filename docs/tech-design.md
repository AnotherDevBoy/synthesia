# Tech Design

## Introduction

The purpose of this document is to showcase the design of the challenge's solution, including tradeoffs made, tech choices, and future improvements.

## Requirements

### Functional

- Expose a /crypto/sign endpoint with a similar input syntax to ours.
- If a result can be provided immediately, include it in the HTTP response with a 200 status code.
- If a result cannot be provided within the required timeframe, respond with a 202 status code, and notify the user with the result when it is ready. This should be implemented by allowing users of your API to specify a webhook notification URL for each request.

### Non-functional

- Your endpoint must always return immediately (within ~2s), regardless of whether the call to our endpoint succeeded.
- You must not hit our endpoint more than 10 times per minute, but you should expect that your endpoint will get bursts of 60 requests in a minute, and still be able to eventually handle all of those.
- You must package your service in such a way that the service can be started as a Docker container or a set of containers with Docker Compose. This will help our engineers when they evaluate your challenge. We will not evaluate challenge solutions that are not containerised.
- [Bonus] If your service shuts down and restarts, users who requested a signature before the shutdown should still be notified when their signature is ready without re-requesting one from scratch.

## Solution

### High-level architecture

At a glance, the application will have two core components:

- **API**. This will expose the `/crypto/sign` as defined by the requirements.
- **Async processor**. This component will be in charge of processing those sign requests that couldn't be fulfilled due to issues with the unreliable API and notify the user.

When the first attempt to sign fails (regardless of the reason), the **API** will delegate to the **Async Processor** further retry attempts. To do so, it will send a message to a queue that's periodically polled by the **Async Processor**.

TODO: Insert diagram

### Detailed design

#### Tech choices

| Technology | Component               | Rationale                                                                                                                  |
| ---------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Java       | API and Async Processor | Well estabilished programming language with good parallelism support                                                       |
| Javalin    | API                     | Lightweight framework for simplicity (for a real app, consider more mature options like Spring Boot or Quarkus)            |
| SQS        | API and Async Processor | For 1 producer and 1 consumer of messages, it is one of the simplest queue options available                               |
| Redis      | CryptoClient            | Open Source in-memory cache that can be used as a backend for Bucket4J and is supported by the majority of cloud providers |

#### Asynchronous Processor

The asynchronous processor has two key components:

- **MessageSigningConsumer**. Reads messages sent by the API to the shared queue (SQS) in bulk and forwards them to the MessageSigningProcessor to process them via a shared in-memory queue (1 by 1).
- **MessageSigningProcessor**. Reads messages sent by the **MessageSigningConsumer** and processes them individually. To process them, it sends another sign request to the unreliable API. If it succeeds, it notifies the user that sent the initial request via the webhook provided (part of the message).

The message itself that's sent first through SQS and eventually through the in-memory queue looks like the following:

```json
{
  "message": "The message to sign",
  "webhookUrl": "https://webhook.com"
}
```

Both, the consumer and processor, run inside of threads in a thread-pool. For the purpose of this solution, I decided to use:

- A fixed thread pool with 1 thread for the consumer.
- A fixed thread pool with 10 threads for the processor.

The reason for that 1/10 balance is the fact that the consumer, at most, will be able to read 10 messages from SQS at a time. On the other hand, for resilience purposes, each processor thread will only process one message at a time. This is to ensure that, if the processing of the message fails again, only one message needs to be retried as opposed to a batch of many.

Both thread pool sizes are configurable through environment variables in case they need to be modified for scalability purposes.

#### Crypto client

The`CryptoClient` is the interface that provides used to abstract away the communication with the unreliable API. It has one implementation, the `HttpRateLimitedCryptoClient` that uses:

- An HTTP client configured with a 2 second timeout. To meet the non-functional requirement of returning immediately (within ~2s)
- A Bucket4j bucket. To meet the non-functional requirement of not hitting the unreliable endpoint more than 10 times per minute.

#### Webhook client

Following the same approach as with the `CryptoClient`, the webhook notification is abstracted away behind the `WebhookClient` interface. An HTTP-based implementation can be found in the `HttpWebhookClient` class.

The definition of the webhook notification mechanism, other than the provided webhook URL being an HTTP URL, was vague. Hence I had to make some assumptions on how it should work based on the URL provided as an argument.

The webhook notification will be a `POST` to the provided webhookURL with the signed message as a query parameter (`signedMessage`). If the webhook returns a status code 300 or greater, it will be considered that the notification failed and it will be retried (after SQS visibility timeout). Otherwise, it will be considered successful.

#### Testing

The business logic has been tested with unit tests, where the dependencies have been mocked using Mockito.

Integration tests have been used to test specific integrations (such as SQS or the HTTP clients).

#### Scalability and bottlenecks

To scale this application further, there are multiple areas that can be improved:

- Since the load patterns may differ, the **API** and the **Asynchronous Processor** pipeline can be decoupled in separate containers.
- The **Asynchronous Processor** can horizontally scale by adding more threads until the container is maximized and adding more instances of the container thereafter.
- To control the number of reappearing messages after multiple failures in a row, a decision can be made to move messages that have been retried multiple times to a DLQ.
- Eventually, the max number of inflight messages for SQS could be hit (120,000). In that case, the timeout for the HTTP client used for webhook notifications can be further reduced to ensure it fails early. If that's not an option, a partition strategy would be required. For example, messages could be split into different queues (or topics if using alternatives like SNS or Kafka) based on the user IP, webhook URL, etc.
- On the rate-limitting side, although Redis is very efficient at performing reads, depending on how the rest of the app is scaled, it could potentially hit a limit on number of connections to the Redis cluster. In that case, more read replicas could be added to the cluster to allow for more connections.
- The main issue with the application is the unreliable endpoint. So another potential solution to avoid bottlenecks or adding complexity would be to run the signing algorithm inside of the API as opposed to relying on an external app.