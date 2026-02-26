package io.opentelemetry.javaagent.tooling.ignore;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesBuilder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesConfigurer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/2/2
 */
@AutoService(IgnoredTypesConfigurer.class)
public class EasyTelemetryIgnoredTypesConfigurer implements IgnoredTypesConfigurer {
  @Override
  public void configure(IgnoredTypesBuilder builder, ConfigProperties config) {
    builder
        .ignoreClass("io.easytelemetry.")
        .ignoreClass("org.benf.cfr.");
  }
}
