package io.opentelemetry.javaagent.etel;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.easytelemetry.instrumentation.api.config.apply.extract.jm.JavaMethodDataExtractor;
import io.easytelemetry.instrumentation.api.config.apply.extract.JavaMethodExtractTimingEnum;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import javax.annotation.Nullable;

public class DataExtractMethodInstrumentation implements TypeInstrumentation {

  private JavaMethodExtractTimingEnum timing;
  private String className;
  private String method;
  private String[] paramTypes;

  public DataExtractMethodInstrumentation(JavaMethodExtractTimingEnum timing, String className, String method, String... paramTypes) {
    this.timing = timing;
    this.className = className;
    this.method = method;
    this.paramTypes = paramTypes;
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named(className);
  }

  @Override
  public void transform(TypeTransformer transformer) {
    ElementMatcher.Junction junction = isMethod().and(named(method));
    junction = EasyTelemetryDataExtractModule.getJunction(junction, paramTypes);

    String enterAdvice = DataExtractMethodInstrumentation.class.getName() + "$EnterAdvice";
    String exitAdvice = DataExtractMethodInstrumentation.class.getName() + "$ExitAdvice";
    if (timing == JavaMethodExtractTimingEnum.ON_ENTER) {
      transformer.applyAdviceToMethod(junction, enterAdvice);
    } else if (timing == JavaMethodExtractTimingEnum.ON_EXIT) {
      transformer.applyAdviceToMethod(junction, exitAdvice);
    } else {
      transformer.applyAdviceToMethod(junction, enterAdvice);
      transformer.applyAdviceToMethod(junction, exitAdvice);
    }
  }

  private static class EnterAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onMethodEnter(final @Advice.Origin Method method, final @Advice.AllArguments Object[] params) {
      JavaMethodDataExtractor.onMethodEnter(method, params);
    }
  }

  private static class ExitAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onMethodExit(
        final @Advice.Origin Method method,
        final @Advice.AllArguments Object[] params,
        final @Advice.Return @Nullable Object returnValue) {
      JavaMethodDataExtractor.onMethodExit(method, params, returnValue);
    }
  }

}
