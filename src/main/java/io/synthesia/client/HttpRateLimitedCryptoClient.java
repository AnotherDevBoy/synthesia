package io.synthesia.client;

import io.github.bucket4j.Bucket;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class HttpRateLimitedCryptoClient implements CryptoClient {
    private HttpClient client;
    private Bucket bucket;
    private String apiKey;

    @Override
    @SneakyThrows
    public Optional<String> sign(String message) {
        try {
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit hit");
                return Optional.empty();
            }

            var encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://hiring.api.synthesia.io/crypto/sign?message=" + encodedMessage))
                    .headers("Authorization", apiKey)
                    .GET()
                    .build();

            var response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Remote API failed");
                return Optional.empty();
            }

            return Optional.of(response.body());
        } catch (Exception e) {
            log.error("Unable to sign message", e);
            return Optional.empty();
        }
    }
}
