package io.synthesia.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.synthesia.Configuration;
import io.synthesia.crypto.CryptoClient;
import io.synthesia.crypto.HttpRateLimitedCryptoClient;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class CryptoModule extends AbstractModule {
  @Provides
  @Singleton
  public CryptoClient cryptoClientProvider() {
    Refill refill = Refill.intervally(10, Duration.ofMinutes(1));
    Bandwidth limit = Bandwidth.classic(10, refill);

    BucketConfiguration configuration = BucketConfiguration.builder().addLimit(limit).build();

    RedisClient redisClient = RedisClient.create(Configuration.getRedisUrl());

    LettuceBasedProxyManager<byte[]> proxyManager =
        LettuceBasedProxyManager.builderFor(redisClient)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                    Duration.ofSeconds(10)))
            .build();

    Bucket bucket =
        proxyManager.builder().build("client".getBytes(StandardCharsets.UTF_8), configuration);

    var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    return new HttpRateLimitedCryptoClient(client, bucket, Configuration.getSynthesiaApiKey());
  }
}
