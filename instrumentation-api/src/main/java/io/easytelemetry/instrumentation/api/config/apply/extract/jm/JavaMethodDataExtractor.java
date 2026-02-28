package io.easytelemetry.instrumentation.api.config.apply.extract.jm;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingSource;
import io.easytelemetry.instrumentation.api.config.apply.extract.json.JsonExtractor;
import io.easytelemetry.instrumentation.api.config.apply.extract.json.JsonTokenExtractor;
import io.easytelemetry.instrumentation.api.utils.SpanContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaMethodDataExtractor {

  private static final Logger logger = Logger.getLogger(JavaMethodDataExtractor.class.getName());

  public static void onMethodEnter(Method method, Object[] params) {
    doJavaMethodParamCollecting(method, params, null, true);
  }

  public static void onMethodExit(Method method, Object[] params, Object ret) {
    doJavaMethodParamCollecting(method, params, ret, false);
  }

  private static void doJavaMethodParamCollecting(Method method, Object[] params, Object ret, boolean executeBefore) {
    // 获取配置
    Map<Integer, Map<String, DataExtractingEntity>> instrumentation = ETelConfig.getJavaMethodExtractingConfigs();
    if (instrumentation.isEmpty()) {
      return;
    }

    // 获取当前根跨度
    Span rootSpan = LocalRootSpan.current();
    if (rootSpan == null || !rootSpan.getSpanContext().isSampled()) {
      return;
    }

    // 获取方法对应的实体列表
    Map<String, DataExtractingEntity> entityList = instrumentation.get(method.hashCode());
    if (entityList == null || entityList.isEmpty()) {  // 添加 null 检查
      return;
    }

    String rootSpanName = SpanContext.getSpanName(rootSpan);
    processEntities(entityList, rootSpanName, rootSpan, params, ret, executeBefore);
  }

  private static void processEntities(Map<String, DataExtractingEntity> entityList,
      String rootSpanName,
      Span rootSpan,
      Object[] params,
      Object ret,
      boolean executeBefore) {
    for (DataExtractingEntity entity : entityList.values()) {  // 使用 values() 避免 entry 的中间对象
      if (!shouldProcessEntity(entity, rootSpanName)) {
        continue;
      }

      processDataExtraction(entity, params, ret, executeBefore, rootSpan);
    }
  }

  private static boolean shouldProcessEntity(DataExtractingEntity entity, String rootSpanName) {
    String spanName = entity.getRootSpanName();
    return spanName == null || rootSpanName.equals(spanName);
  }

  private static void processDataExtraction(DataExtractingEntity entity,
      Object[] params,
      Object ret,
      boolean executeBefore,
      Span rootSpan) {
    if (entity.getSourceType() == DataExtractingSource.JAVA_METHOD_RETURN_VALUE) {
      if (!executeBefore) {
        populateTags(ret, entity, rootSpan);
      }
    } else {
      boolean hasTimingFlag = entity.getJavaMethodMeta().containsKey("timing");
      boolean shouldExtract = (executeBefore && !hasTimingFlag) || (!executeBefore && hasTimingFlag);
      if (shouldExtract) {
        populateTags(params, entity, rootSpan);
      }
    }
  }


  private static void populateTags(Object value, DataExtractingEntity entity, Span span) {
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

    Object tagValue = value;
    for (JsonTokenExtractor extractor : extractors) {
      tagValue = extractor.execute(tagValue);
    }

    if (tagValue == null) {
      return;
    }
    AttributeKey key = entity.getAttributeKey();
    if (key == null) {
      key = JsonExtractor.buildAttributeKey(tagValue, entity.getTagKey());
      if (key == null) {
        logger.log(Level.SEVERE, "Illegal json expression! Final data isn not primitive or string!");
        return;
      }
      entity.setAttributeKey(key);
    }
    span.setAttribute(key, entity.convertTagValue(tagValue));
  }
}
