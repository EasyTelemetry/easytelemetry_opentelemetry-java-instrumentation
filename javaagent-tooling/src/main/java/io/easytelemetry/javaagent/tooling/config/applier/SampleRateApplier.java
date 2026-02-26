package io.easytelemetry.javaagent.tooling.config.applier;

import io.easytelemetry.instrumentation.api.config.ETelConfigApplier;
import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.easytelemetry.javaagent.tooling.sample.ETelParentBasedSampler;
import io.easytelemetry.javaagent.tooling.sample.ETelSampler;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/6
 */
public class SampleRateApplier implements ETelConfigApplier {

  private static final Logger logger = Logger.getLogger(SampleRateApplier.class.getName());

  public static final SampleRateApplier INSTANCE = new SampleRateApplier();

  private static final String SAMPLE_RATE_KEY = "sample_rate";
  private static final String SERVICE_SAMPLE_RATE_KEY = "service_sample_rate";

  private Sampler rawSampler;
  private ETelSampler eTelSampler;

  private static SdkTracerProvider tracerProvider;

  @Override
  public void apply(Map<String, Object> config) throws Throwable {
    Object rate = config.get(SAMPLE_RATE_KEY);
    if (rate != null && !(rate instanceof Double)) {
      logger.log(Level.WARNING, "sample_rate must be a double");
      return;
    }
    Object serviceRate = config.get(SERVICE_SAMPLE_RATE_KEY);
    if (serviceRate != null && !(serviceRate instanceof Map)) {
      logger.log(Level.WARNING, "service_sample_rate must be a map");
      return;
    }
    Map<String, Double> serviceRateValue = (Map<String, Double>) serviceRate;
    if (rate == null && (serviceRateValue == null || serviceRateValue.isEmpty())) {
      if (eTelSampler != null) {
        Object state = ReflectionUtils.get(tracerProvider, "sharedState");
        ReflectionUtils.set(state, "sampler", rawSampler);
      }
      return;
    }

    float rateValue;
    if (rate == null) {
      rateValue = 1.0F;
    } else {
      rateValue = ((Double) rate).floatValue();
      if (rateValue > 1 || rateValue < 0) {
        logger.log(Level.WARNING, "sample_rate must be between 0 and 1");
        return;
      }
    }

    if (eTelSampler == null) {
      eTelSampler = new ETelSampler(rateValue, castServiceRateMap(serviceRateValue));
      installETelSampler(eTelSampler);
    } else {
      eTelSampler.updateGlobalSampleRate(rateValue);
      eTelSampler.updateServiceSampleRate(castServiceRateMap(serviceRateValue));
    }
  }

  private void installETelSampler(ETelSampler eTelSampler) {
    Object state = ReflectionUtils.get(tracerProvider, "sharedState");
    rawSampler = ReflectionUtils.get(state, "sampler");
    ETelParentBasedSampler eTelParentBasedSampler = new ETelParentBasedSampler(eTelSampler,
        ReflectionUtils.get(rawSampler, "remoteParentSampled"),
        ReflectionUtils.get(rawSampler, "remoteParentNotSampled"),
        ReflectionUtils.get(rawSampler, "localParentSampled"),
        ReflectionUtils.get(rawSampler, "localParentNotSampled"));
    ReflectionUtils.set(state, "sampler", eTelParentBasedSampler);
  }

  private Map<String, Integer> castServiceRateMap(Map<String, Double> serviceRateValue) {
    Map<String, Integer> serviceRate = new HashMap<>(serviceRateValue.size());
    for (Map.Entry<String, Double> entry : serviceRateValue.entrySet()) {
      serviceRate.put(entry.getKey(), (int) (entry.getValue().floatValue() * 1000));
    }
    return serviceRate;
  }

  public static void setAutoConfiguredOpenTelemetrySdk(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    tracerProvider = ReflectionUtils.getFieldValues(autoConfiguredOpenTelemetrySdk,
        "openTelemetrySdk", "tracerProvider", "delegate");
  }
}
