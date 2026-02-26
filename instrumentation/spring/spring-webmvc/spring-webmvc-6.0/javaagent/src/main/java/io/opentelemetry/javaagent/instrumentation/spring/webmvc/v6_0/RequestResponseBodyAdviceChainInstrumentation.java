package io.opentelemetry.javaagent.instrumentation.spring.webmvc.v6_0;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.easytelemetry.instrumentation.api.config.apply.extract.json.JsonBodyDataExtractor;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class RequestResponseBodyAdviceChainInstrumentation implements TypeInstrumentation {



  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named(
        "org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyAdviceChain");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("processBody"))
            .and(ElementMatchers.isPrivate()),
        this.getClass().getName() + "$ResponseBodyAdvice");
  }


  public static class ResponseBodyAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Return Object body) {
      if(body != null){
        JsonBodyDataExtractor.extractDataFromBody(body, false);
      }
    }
  }

}
