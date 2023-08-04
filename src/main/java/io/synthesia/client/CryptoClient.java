package io.synthesia.client;

import java.util.Optional;

public interface CryptoClient {
    Optional<String> sign(String message);
}
