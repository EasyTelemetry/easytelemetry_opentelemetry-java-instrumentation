package io.easytelemetry.instrumentation.api.config.apply.extract.json;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/25
 */
public class ThisTokenExtractor implements JsonTokenExtractor {

  public static final ThisTokenExtractor INSTANCE = new ThisTokenExtractor();

  @Override
  public Object execute(Object target) {
    return target;
  }
}
