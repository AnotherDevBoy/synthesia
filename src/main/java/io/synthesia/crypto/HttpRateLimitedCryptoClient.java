package io.synthesia.crypto;

import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Counter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class HttpRateLimitedCryptoClient implements CryptoClient {
  private final HttpClient client;
  private final Bucket bucket;
  private final String baseUrl;
  private final String apiKey;
  private final Duration timeout;

  private final Counter success;
  private final Counter errors;
  private final Counter apiRateLimitError;
  private final Counter rateLimit;

  @Override
  @SneakyThrows
  public Optional<String> sign(String message) {
    try {
      if (!bucket.tryConsume(1)) {
        this.rateLimit.increment();
        return Optional.empty();
      }

      var encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(baseUrl + "/crypto/sign?message=" + encodedMessage))
              .headers("Authorization", apiKey)
              .timeout(timeout)
              .GET()
              .build();

      var response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.warn(
            "Remote API failed with status {} and error {}",
            response.statusCode(),
            response.body());
        this.errors.increment();

        if (response.statusCode() == 429) {
          this.apiRateLimitError.increment();
        }

        return Optional.empty();
      }

      success.increment();
      return Optional.of(response.body());
    } catch (Exception e) {
      log.error("Unable to sign message", e);
      return Optional.empty();
    }
  }
}
