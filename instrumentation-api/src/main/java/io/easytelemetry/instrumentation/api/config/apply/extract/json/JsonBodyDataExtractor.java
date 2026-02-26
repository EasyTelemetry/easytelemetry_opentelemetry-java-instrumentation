package io.easytelemetry.instrumentation.api.config.apply.extract.json;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntityComposite;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingSource;
import io.easytelemetry.instrumentation.api.utils.SpanContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/2
 */
public class JsonBodyDataExtractor {

  private static final Logger logger = Logger.getLogger(JsonBodyDataExtractor.class.getName());

  public static void extractDataFromBody(Object body, boolean fromRequestBody) {
    DataExtractingEntityComposite entityComposite = ETelConfig.getDataExtractingConfigs();
    if (!entityComposite.isHasBodyConfig()) {
      return;
    }
    Span rootSpan = LocalRootSpan.current();
    if (rootSpan == null || !rootSpan.getSpanContext().isSampled()) {
      return;
    }
    DataExtractingEntity[] entities;
    if (fromRequestBody) {
      entities = entityComposite.getRequestBodyDataExtractingEntities();
    } else {
      entities = entityComposite.getResponseBodyDataExtractingEntities();
    }
    String rootSpanName = SpanContext.getSpanName(rootSpan);
    for (DataExtractingEntity entity : entities) {
      if ((fromRequestBody && entity.getSourceType() == DataExtractingSource.HTTP_REQUEST_BODY) ||
          (!fromRequestBody && entity.getSourceType() == DataExtractingSource.HTTP_RESPONSE_BODY)) {
        String spanName = entity.getRootSpanName();
        if (spanName != null && !spanName.equals(rootSpanName)) {
          continue;
        }

        JsonTokenExtractor[] extractors = entity.getJsonTokenExtractors();
        if (extractors == null) {
          extractors = JsonExtractor.parseExpression(entity.getExpression());
          if (extractors != null) {
            entity.setJsonTokenExtractors(extractors);
          }
        }

        if (extractors == null) {
          logger.log(Level.SEVERE, "Illegal json expression: " + entity.getExpression());
          return;
        }

        Object tagValue = body;
        for (JsonTokenExtractor extractor : extractors) {
          tagValue = extractor.execute(tagValue);
        }

        if (tagValue == null) {
          return;
        }
        AttributeKey key = entity.getAttributeKey();
        if (key != null) {
          rootSpan.setAttribute(key, tagValue);
          return;
        }
        key = JsonExtractor.commonSetTag(tagValue, entity.getTagKey(), rootSpan);
        if (key == null) {
          logger.log(Level.SEVERE, "Illegal json expression! Final data isn not primitive or string!");
          continue;
        }
        entity.setAttributeKey(key);
      }
    }
  }

}
