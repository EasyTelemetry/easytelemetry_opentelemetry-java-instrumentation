package io.opentelemetry.javaagent.instrumentation.spring.webmvc.v6_0;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.easytelemetry.instrumentation.api.config.apply.extract.json.JsonBodyDataExtractor;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.http.HttpInputMessage;

public class RequestBodyMethodProcessorInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named(
        "org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodArgumentResolver");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isProtected())
            .and(named("readWithMessageConverters")),
        this.getClass().getName() + "$RequestBodyAdvice");
  }

  public static class RequestBodyAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Argument(0) Object arg1, @Advice.Return Object obj) {
      if (arg1 != null && arg1 instanceof HttpInputMessage) {
        JsonBodyDataExtractor.extractDataFromBody(obj, true);
      }
    }
  }

}
