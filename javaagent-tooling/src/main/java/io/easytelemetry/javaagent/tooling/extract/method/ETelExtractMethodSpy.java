package io.easytelemetry.javaagent.tooling.extract.method;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.spy.SpyAPI;
import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.easytelemetry.instrumentation.api.utils.SpanContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/26
 */
public class ETelExtractMethodSpy extends SpyAPI.AbstractExtractSpy {


  private static final Logger logger = Logger.getLogger(ETelExtractMethodSpy.class.getName());

  public static final String ADVICE_CLASS = SpyAPI.class.getName().replace(".", "/");

  public static Method extractParamMethod;
  public static Method extractReturnMethod;

  private static SpyAPI.AbstractExtractSpy spy = new ETelExtractMethodSpy();

  static {
    SpyAPI.setExtractMethodSpy(spy);
    extractParamMethod = ReflectionUtils.findMethod(SpyAPI.class, "extractParam", int.class, Object.class);
    extractReturnMethod = ReflectionUtils.findMethod(SpyAPI.class, "extractReturn", int.class, Object.class);
  }

  @Override
  public void extractParam(int index, Object param) {
    if (param == null) {
      return;
    }
    try {
      DataExtractingEntity entity = ETelConfig.getIndexedMethodExtractingEntities()[index];
      Span span = checkExtractEnable(entity);
      if (span != null) {
        entity.doExtract(true,param, span);
      }
    } catch (Throwable throwable) {
      logger.log(Level.SEVERE, "Error when extracting param", throwable);
    }
  }

  @Override
  public void extractReturn(int index, Object ret) {
    try {
      extractParam(index, ret);
    } catch (Throwable throwable) {
      logger.log(Level.SEVERE, "Error when extracting return", throwable);
    }
  }

  private static Span checkExtractEnable(DataExtractingEntity entity) {
    Span rootSpan = LocalRootSpan.current();
    if (rootSpan == null || !rootSpan.getSpanContext().isSampled()) {
      return null;
    }
    String spanName = entity.getRootSpanName();
    if (spanName != null && !spanName.equals(SpanContext.getSpanName(rootSpan))) {
      return null;
    }
    return rootSpan;
  }
}
