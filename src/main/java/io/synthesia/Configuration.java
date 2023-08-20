package io.synthesia;

public final class Configuration {
  public static String getSynthesiaApiKey() {
    return System.getenv("API_KEY");
  }

  public static String getRedisUrl() {
    String maybeRedisUrl = System.getenv("REDIS_URL");

    return maybeRedisUrl == null ? "redis://localhost:6379" : maybeRedisUrl;
  }

  public static String getSqsUrl() {
    String maybeSqsUrl = System.getenv("SQS_URL");

    return maybeSqsUrl == null ? "http://localhost:4566" : maybeSqsUrl;
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

  public static String getApiBaseURL() {
    String maybeApiBaseURL = System.getenv("API_BASE_URL");

    return maybeApiBaseURL == null ? "https://hiring.api.synthesia.io" : maybeApiBaseURL;
  }

  public static int getClientTimeoutInSeconds() {
    String maybeTimeoutInSeconds = System.getenv("CLIENT_TIMEOUT");

    return maybeTimeoutInSeconds == null ? 2 : Integer.parseInt(maybeTimeoutInSeconds);
  }
}
