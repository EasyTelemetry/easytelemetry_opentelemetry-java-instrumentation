package io.easytelemetry.instrumentation.api.config.apply.extract.http;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntityComposite;
import io.easytelemetry.instrumentation.api.config.apply.extract.RequestDataExtractor;
import io.easytelemetry.instrumentation.api.utils.SpanContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/31
 */
public class HttpRequestDataExtractor<T> implements RequestDataExtractor<T> {

  private static final Logger logger = Logger.getLogger(HttpRequestDataExtractor.class.getName());

  private HttpRequestDataFetcher fetcher;

  public HttpRequestDataExtractor(HttpRequestDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  @Override
  public void doDataExtracting(T request, Span span, Context context) {
    if (fetcher.hasFetchedBefore(request)) {
      return;
    }
    try {
      doDataExtractingInternal(request, context);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Extracting request data error", e);
    } finally {
      fetcher.markFetchedFlag(request);
    }
  }

  private void doDataExtractingInternal(T request, Context context) {

    DataExtractingEntityComposite configs = ETelConfig.getDataExtractingConfigs();
    if (configs == null || !configs.isHasContextConfig()) {
      return;
    }

    Span rootSpan = LocalRootSpan.fromContextOrNull(context);
    if (rootSpan == null) {
      Context root = Context.root();
      rootSpan = root.get(SpanContext.key);
    }
    if (rootSpan == null) {
      return;
    }
    String rootSpanName = SpanContext.getSpanName(rootSpan);

    applyExtracting(rootSpanName, request, rootSpan, configs.getRequestParamDataExtractingEntities(),
        (request1, expression) -> fetcher.getRequestParameter(request1, expression));

    applyExtracting(rootSpanName, request, rootSpan, configs.getRequestHeaderDataExtractingEntities(),
        (request1, expression) -> fetcher.getRequestHeader(request1, expression));

    applyExtracting(rootSpanName, request, rootSpan, configs.getRequestUriDataExtractingEntities(),
        (request1, expression) -> fetcher.getRequestUrl(request1));
  }

  private AttributeKey getOrPopulateAttributeKey(DataExtractingEntity config) {
    AttributeKey key = config.getAttributeKey();
    if (key == null) {
      key = AttributeKey.stringKey(config.getTagKey());
      config.setAttributeKey(key);
    }
    return key;
  }

  private void applyExtracting(String rootSpanName, T request, Span rootSpan,
      DataExtractingEntity[] entities,
      HttpDataExtractFunction<T, String, String> function) {
    if (entities != null || entities.length > 0) {
      for (DataExtractingEntity config : entities) {
        String spanName = config.getRootSpanName();
        if (spanName != null && !spanName.equals(rootSpanName)) {
          continue;
        }
        AttributeKey key = getOrPopulateAttributeKey(config);
        String data = function.apply(request, config.getExpression());
        if (data != null) {
          rootSpan.setAttribute(key, data);
        }
      }
    }
  }

  private interface HttpDataExtractFunction<T, E, R> {
    R apply(T t, E e);
  }

}
