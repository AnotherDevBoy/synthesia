package io.synthesia.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.synthesia.Configuration;
import io.synthesia.crypto.CryptoClient;
import io.synthesia.crypto.HttpRateLimitedCryptoClient;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class CryptoModule extends AbstractModule {
  @Provides
  @Singleton
  public CryptoClient cryptoClientProvider(MeterRegistry meterRegistry) {
    Bandwidth limit = Bandwidth.simple(5, Duration.ofMinutes(1));

    BucketConfiguration configuration = BucketConfiguration.builder().addLimit(limit).build();

    RedisClient redisClient = RedisClient.create(Configuration.getRedisUrl());

    LettuceBasedProxyManager<byte[]> proxyManager =
        LettuceBasedProxyManager.builderFor(redisClient)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                    Duration.ofMinutes(2)))
            .build();

    Bucket bucket =
        proxyManager.builder().build("client".getBytes(StandardCharsets.UTF_8), configuration);

    Counter success = Counter.builder("client_success").register(meterRegistry);
    Counter clientErrorsCounter = Counter.builder("client_error").register(meterRegistry);
    Counter apiRateLimitErrorCounter =
        Counter.builder("client_api_rate_limit_error").register(meterRegistry);
    Counter rateLimitCounter = Counter.builder("client_rate_limit_count").register(meterRegistry);

    var client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Configuration.getClientTimeoutInSeconds()))
            .build();

    return new HttpRateLimitedCryptoClient(
        client,
        bucket,
        Configuration.getApiBaseURL(),
        Configuration.getSynthesiaApiKey(),
        Duration.ofSeconds(Configuration.getClientTimeoutInSeconds()),
        success,
        clientErrorsCounter,
        apiRateLimitErrorCounter,
        rateLimitCounter);
  }
}
