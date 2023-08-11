package io.synthesia.crypto;

public class HttpRateLimitedCryptoClientTest {
  // TODO: Make base URI configurable
  // TODO: Use WireMock
  // sign returns empty, if tryconsume fails
  // sign returns empty, if tryconsume throws
  // sign returns empty if client.send throws
  // sign returns empty, if status code is greater than 300
  // sign returns the content of the body if status code 200
}
