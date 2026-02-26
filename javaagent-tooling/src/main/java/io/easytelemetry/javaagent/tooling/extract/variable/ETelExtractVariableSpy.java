package io.easytelemetry.javaagent.tooling.extract.variable;

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
 * @since 2026/1/23
 */
public class ETelExtractVariableSpy extends SpyAPI.AbstractExtractVariableSpy {

  private static final Logger logger = Logger.getLogger(ETelExtractVariableSpy.class.getName());

  public static final String ADVICE_CLASS = SpyAPI.class.getName().replace(".", "/");

  public static Method extractVariableMethod;

  private static SpyAPI.AbstractExtractVariableSpy spy = new ETelExtractVariableSpy();

  static {
    extractVariableMethod = ReflectionUtils.findMethod(SpyAPI.class, "extractVariable", int.class, Object.class);

    SpyAPI.setExtractVariableSpy(spy);
  }

  @Override
  public void extractVariable(int index, Object variable) {
    if (variable == null){
      return;
    }
    try {
      DataExtractingEntity entity = ETelConfig.getIndexedVariableExtractingEntities()[index];
      Span span = checkExtractEnable(entity);
      if (span != null) {
        entity.doExtract(false, variable, span);
      }
    } catch (Throwable throwable) {
      logger.log(Level.SEVERE, "Error when extracting local variable", throwable);
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
