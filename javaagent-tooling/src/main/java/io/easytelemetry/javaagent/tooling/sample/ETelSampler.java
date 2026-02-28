package io.easytelemetry.javaagent.tooling.sample;

import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.UrlAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/5
 */
public class ETelSampler implements Sampler {

  private static Class traceIdRatioBasedSamplerClass;

  private Sampler traceIdRatioBasedSampler;
  private Map<String, Integer> serviceSamplingRate;
  private Map<String, LongAdder> serviceRequestCounter;

  static {
    try {
      traceIdRatioBasedSamplerClass = Class.forName("io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler");
    } catch (ClassNotFoundException e) {
      // ignore
    }
  }

  public ETelSampler(float samplingRate, Map<String, Integer> serviceSamplingRate) {
    this.traceIdRatioBasedSampler = createTraceIdRatioBasedSampler(samplingRate);
    this.serviceSamplingRate = serviceSamplingRate;
    this.serviceRequestCounter = new ConcurrentHashMap<>(serviceSamplingRate.size());
    for (Map.Entry<String, Integer> entry : serviceSamplingRate.entrySet()) {
      serviceRequestCounter.put(entry.getKey(), new LongAdder());
    }
  }

  public void updateGlobalSampleRate(float samplingRate) {
    this.traceIdRatioBasedSampler = createTraceIdRatioBasedSampler(samplingRate);
  }

  public void updateServiceSampleRate(Map<String, Integer> serviceSamplingRate) {
    this.serviceSamplingRate = serviceSamplingRate;
    for (Map.Entry<String, Integer> entry : serviceSamplingRate.entrySet()) {
      serviceRequestCounter.putIfAbsent(entry.getKey(), new LongAdder());
    }
    Set<String> set = new HashSet<>(serviceRequestCounter.keySet());
    set.removeAll(serviceSamplingRate.keySet());
    for (String s : set) {
      serviceRequestCounter.remove(s);
    }
  }

  @Override
  public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
      Attributes attributes, List<LinkData> parentLinks) {
    if ((spanKind != SpanKind.SERVER && spanKind != SpanKind.CONSUMER) || serviceSamplingRate == null || serviceSamplingRate.isEmpty()) {
      return traceIdRatioBasedSampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    String spanName = name;
    if (spanKind == SpanKind.SERVER) {
      char c = name.charAt(0);
      if(c == 'G' || c == 'P' || c == 'D'){
        String path = attributes.get(UrlAttributes.URL_PATH);
        if (path == null) {
          return traceIdRatioBasedSampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
        }
        spanName = new StringBuilder(name).append(" ").append(path).toString();
      }
    }
    Integer rate = serviceSamplingRate.get(spanName);
    if (rate == null) {
      return traceIdRatioBasedSampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    LongAdder counter = serviceRequestCounter.get(spanName);
    if (counter == null) {
      serviceRequestCounter.put(spanName, new LongAdder());
      counter = serviceRequestCounter.get(spanName);
    }
    return shouldSample(rate, counter) ? SamplingResult.recordAndSample() : SamplingResult.drop();
  }

  private boolean shouldSample(int threshold, LongAdder counter) {
    counter.increment();
    return (counter.longValue() + System.nanoTime() % 1000) % 1000 < threshold;
  }

  private static Sampler createTraceIdRatioBasedSampler(double samplingRate) {
    try {
      return ReflectionUtils.invokeStatic(traceIdRatioBasedSamplerClass, "create", samplingRate);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String getDescription() {
    return "TEelSampler";
  }

}
