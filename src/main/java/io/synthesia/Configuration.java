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
}
