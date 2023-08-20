package io.synthesia;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.synthesia.api.SignApi;
import io.synthesia.api.WebhookApi;
import io.synthesia.async.MessageSigningConsumer;
import io.synthesia.async.MessageSingingProcessor;
import io.synthesia.di.AsyncModule;
import io.synthesia.di.CryptoModule;
import io.synthesia.di.MetricsModule;
import io.synthesia.di.QueueModule;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
  public static void main(String[] args) {
    Injector injector =
        Guice.createInjector(
            new MetricsModule(), new CryptoModule(), new QueueModule(), new AsyncModule());

    SignApi signApi = injector.getInstance(SignApi.class);
    WebhookApi webhookApi = injector.getInstance(WebhookApi.class);

    PrometheusMeterRegistry registry =
        (PrometheusMeterRegistry) injector.getInstance(MeterRegistry.class);

    MicrometerPlugin micrometerPlugin =
        MicrometerPlugin.Companion.create(micrometerConfig -> micrometerConfig.registry = registry);

    Javalin app =
        Javalin.create(config -> config.plugins.register(micrometerPlugin))
            .get("/webhook", webhookApi::notify)
            .get("/crypto/sign", signApi::sign)
            .get(
                "/prometheus",
                ctx -> ctx.contentType(TextFormat.CONTENT_TYPE_004).result(registry.scrape()))
            .start(7070);

    MessageSigningConsumer messageSigningConsumer =
        injector.getInstance(MessageSigningConsumer.class);
    MessageSingingProcessor messageSingingProcessor =
        injector.getInstance(MessageSingingProcessor.class);

    ExecutorService consumerPool =
        Executors.newFixedThreadPool(Configuration.getConsumerPoolSize());
    consumerPool.execute(messageSigningConsumer);

    ExecutorService processorPool =
        Executors.newFixedThreadPool(Configuration.getProcessorPoolSize());

    IntStream.range(0, 10).forEach(i -> processorPool.execute(messageSingingProcessor));

    scheduleShutdown(app, consumerPool, processorPool);
  }

  private static void scheduleShutdown(
      Javalin app, ExecutorService consumerPool, ExecutorService processorPool) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    app.stop();

                    consumerPool.shutdown();
                    processorPool.shutdown();

                    consumerPool.awaitTermination(2, TimeUnit.SECONDS);
                    processorPool.awaitTermination(2, TimeUnit.SECONDS);

                    log.info("Shutdown completed");
                  } catch (InterruptedException e) {
                    log.error("An error occurred during shutdown", e);
                    throw new RuntimeException(e);
                  }
                }));
  }
}
