package io.opentelemetry.javaagent.etel;

import static java.util.Arrays.asList;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import io.easytelemetry.instrumentation.api.config.apply.extract.JavaMethodExtractTimingEnum;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.Ordered;
import java.util.Arrays;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/3
 */
public class EasyTelemetryDataExtractModule extends InstrumentationModule implements Ordered {

  private boolean isStatic;
  private String className;
  private String method;
  private String[] paramTypes;
  private JavaMethodExtractTimingEnum timing;

  public EasyTelemetryDataExtractModule() {
    super("etel-data-extractor");
  }

  public EasyTelemetryDataExtractModule(String className, String method, String[] paramTypes, boolean isStatic,
      JavaMethodExtractTimingEnum timing) {
    super(className + "#" + method);
    this.isStatic = isStatic;
    this.className = className;
    this.method = method;
    this.paramTypes = paramTypes;
    this.timing = timing;
  }

  @Override
  public boolean defaultEnabled(ConfigProperties config) {
    return className != null;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return isStatic ? asList(new DataExtractStaticMethodInstrumentation(timing, className, method, paramTypes))
        : Arrays.asList(new DataExtractMethodInstrumentation(timing, className, method, paramTypes));
  }

  public boolean isIndyModule() {
    return false;
  }

  static ElementMatcher.Junction getJunction(ElementMatcher.Junction junction, String[] paramTypes) {
    if (paramTypes == null || paramTypes.length == 0) {
      junction = junction.and(takesNoArguments());
    } else {
      junction = junction.and(takesArguments(paramTypes.length));
      for (int i = 0; i < paramTypes.length; i++) {
        String paramType = paramTypes[i];
        if (paramType.contains("<")) {
          paramType = paramType.substring(0, paramType.indexOf("<"));
        }
        junction = junction.and(takesArgument(i, named(paramType)));
      }
    }
    return junction;
  }
}
