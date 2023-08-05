package io.synthesia.crypto;

import java.util.Optional;

public interface CryptoClient {
  Optional<String> sign(String message);
}
