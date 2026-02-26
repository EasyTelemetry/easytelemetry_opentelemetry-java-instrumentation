package io.easytelemetry.instrumentation.api.utils;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.ContextKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/31
 */
public class SpanContext {

  public static ContextKey<Span> key;
  private static Field spanNameField;

  private static Method getAttributeMethod;

  static {
    Class<?> clazz = null;
    try {
      clazz = Class.forName("io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.SpanContextKey");
    } catch (Exception e) {
      try {
        clazz = Class.forName("io.opentelemetry.api.trace.SpanContextKey");
      } catch (ClassNotFoundException ex) {
        //ignore
      }
    }
    try {
      key = ReflectionUtils.getStatic(clazz, "KEY");
    } catch (Exception e) {
      // ignore
    }
  }

  public static String getSpanName(Span span) {
    if (spanNameField == null) {
      try {
        spanNameField = ReflectionUtils.findField(span.getClass(), "name");
        spanNameField.setAccessible(true);
      } catch (Exception e) {
        // ignore
      }
    }
    return ReflectionUtils.getField(spanNameField, span);
  }

  public static <T> T getAttribute(AttributeKey<T> key, Span span) {
    if (getAttributeMethod == null) {
      try {
        getAttributeMethod = ReflectionUtils.findMethod(span.getClass(), "getAttribute", AttributeKey.class);
        getAttributeMethod.setAccessible(true);
      } catch (Exception e) {
        // ignore
      }
    }
    return (T) ReflectionUtils.invokeMethod(getAttributeMethod, span, key);
  }

}
