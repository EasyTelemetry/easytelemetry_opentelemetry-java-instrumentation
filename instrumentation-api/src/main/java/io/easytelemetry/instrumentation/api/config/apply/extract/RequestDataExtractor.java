package io.easytelemetry.instrumentation.api.config.apply.extract;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

public interface RequestDataExtractor<T> {

  void doDataExtracting(T request, Span span, Context context);

}
