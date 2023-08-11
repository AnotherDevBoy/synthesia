package io.synthesia;

public final class Configuration {
  public static String getSynthesiaApiKey() {
    return System.getenv("API_KEY");
  }

  public static String getRedisUrl() {
    return System.getenv("REDIS_URL");
  }

  public static String getSqsUrl() {
    return System.getenv("SQS_URL");
  }

  public static int getAsyncQueueSize() {
    String maybeQueueSize = System.getenv("ASYNC_QUEUE_SIZE");

    return maybeQueueSize == null ? 100 : Integer.parseInt(maybeQueueSize);
  }

  public static int getConsumerPoolSize() {
    String maybePoolSize = System.getenv("CONSUMER_POOL_SIZE");

    return maybePoolSize == null ? 1 : Integer.parseInt(maybePoolSize);
  }

  public static int getProcessorPoolSize() {
    String maybePoolSize = System.getenv("PROCESSOR_POOL_SIZE");

    return maybePoolSize == null ? 10 : Integer.parseInt(maybePoolSize);
  }
}
