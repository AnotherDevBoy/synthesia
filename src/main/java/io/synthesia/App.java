package io.synthesia;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import io.synthesia.api.SignApi;
import io.synthesia.api.WebhookApi;
import io.synthesia.async.MessageSigningConsumer;
import io.synthesia.async.MessageSingingProcessor;
import io.synthesia.di.AsyncModule;
import io.synthesia.di.CryptoModule;
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
        Guice.createInjector(new CryptoModule(), new QueueModule(), new AsyncModule());

    SignApi signApi = injector.getInstance(SignApi.class);
    WebhookApi webhookApi = injector.getInstance(WebhookApi.class);

    Javalin app =
        Javalin.create()
            .get("/webhook", webhookApi::notify)
            .post("/crypto/sign", signApi::sign)
            .start(7070);

    MessageSigningConsumer messageSigningConsumer =
        injector.getInstance(MessageSigningConsumer.class);
    MessageSingingProcessor messageSingingProcessor =
        injector.getInstance(MessageSingingProcessor.class);

    ExecutorService consumerPool = Executors.newSingleThreadExecutor();
    consumerPool.execute(messageSigningConsumer);

    ExecutorService processorPool = Executors.newFixedThreadPool(10);

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
