package io.synthesia;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.javalin.Javalin;
import io.lettuce.core.RedisClient;
import io.synthesia.api.SignApi;
import io.synthesia.crypto.CryptoClient;
import io.synthesia.crypto.HttpRateLimitedCryptoClient;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class App {
  public static void main(String[] args) {
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

    CryptoClient cryptoService =
        new HttpRateLimitedCryptoClient(client, bucket, Configuration.getSynthesiaApiKey());
    SignApi signApi = new SignApi(cryptoService);

    var app = Javalin.create().post("/crypto/sign", signApi::sign).start(7070);

    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
  }
}
