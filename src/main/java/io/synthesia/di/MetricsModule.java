package io.synthesia.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsModule extends AbstractModule {

  @Provides
  @Singleton
  public MeterRegistry meterRegistryProvider() {
    MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    return registry;
  }
}
