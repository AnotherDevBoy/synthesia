package io.synthesia.crypto;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class HttpRateLimitedCryptoClientTest {
  private static final int TIMEOUT_IN_SECONDS = 1;

  @Mock private Bucket bucket;

  private WireMockServer wireMockServer;

  private HttpRateLimitedCryptoClient sut;

  @BeforeEach
  void beforeEach() {
    MockitoAnnotations.initMocks(this);

    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    this.wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    this.wireMockServer.start();
    String baseUrlString = this.wireMockServer.baseUrl();

    MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    this.sut =
        new HttpRateLimitedCryptoClient(
            httpClient,
            this.bucket,
            baseUrlString,
            "",
            Duration.ofSeconds(TIMEOUT_IN_SECONDS),
            Counter.builder("").register(registry),
            Counter.builder("").register(registry),
            Counter.builder("").register(registry),
            Counter.builder("").register(registry));
  }

  @AfterEach
  void afterEach() {
    this.wireMockServer.stop();
  }

  @Test
  void sign_whenTryConsumeFails_returnsEmpty() {
    whenBucketTryConsumeFails();

    assertSignReturnsEmpty("message");
  }

  @Test
  void sign_whenTryConsumeThrows_returnsEmpty() {
    whenBucketTryConsumeThrows();

    assertSignReturnsEmpty("message");
  }

  @Test
  void sign_whenClientTimesOut_returnsEmpty() {
    whenBucketTryConsumeSucceeds();
    whenRemoteApiReturnsLateResponse();

    assertSignReturnsEmpty("message");
  }

  @Test
  void sign_whenStatusCodeNot200_returnsEmpty() {
    whenBucketTryConsumeSucceeds();
    whenRemoteApiFails();

    assertSignReturnsEmpty("message");
  }

  @Test
  void sign_whenStatus200_returnsResponseBody() {
    whenBucketTryConsumeSucceeds();
    whenRemoteApiReturnsResponse();

    assertSignReturnsSignedMessage("message", "signed message");
  }

  private void whenBucketTryConsumeFails() {
    when(this.bucket.tryConsume(anyLong())).thenReturn(false);
  }

  private void whenBucketTryConsumeThrows() {
    when(this.bucket.tryConsume(anyLong())).thenThrow(RuntimeException.class);
  }

  private void whenBucketTryConsumeSucceeds() {
    when(this.bucket.tryConsume(anyLong())).thenReturn(true);
  }

  private void whenRemoteApiReturnsResponse() {
    this.wireMockServer.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/crypto/sign"))
            .withQueryParam("message", WireMock.equalTo("message"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("signed message")));
  }

  private void whenRemoteApiFails() {
    this.wireMockServer.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/crypto/sign"))
            .willReturn(WireMock.aResponse().withStatus(300)));
  }

  private void whenRemoteApiReturnsLateResponse() {
    this.wireMockServer.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/crypto/sign"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody("signed message")
                    .withFixedDelay((int) Duration.ofSeconds(TIMEOUT_IN_SECONDS + 1).toMillis())));
  }

  private void assertSignReturnsEmpty(String message) {
    var maybeSignedMessage = this.sut.sign(message);

    assertEquals(Optional.empty(), maybeSignedMessage);
  }

  private void assertSignReturnsSignedMessage(String message, String signedMessage) {
    var maybeSignedMessage = this.sut.sign("message");

    assertTrue(maybeSignedMessage.isPresent());
    assertEquals("signed message", maybeSignedMessage.get());
  }
}
