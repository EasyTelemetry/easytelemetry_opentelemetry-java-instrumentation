package io.easytelemetry.instrumentation.api.config.apply.extract.json;

import io.opentelemetry.api.common.AttributeKey;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/1
 */
public class JsonExtractor {

  private JsonExtractor() {
  }


  // $.[0][1].users[1].name
  public static JsonTokenExtractor[] parseExpression(String exp) {
    if (exp.startsWith("$.")) {
      exp = exp.substring(2);
    }
    if (exp.length() == 0) {
      return new JsonTokenExtractor[] {ThisTokenExtractor.INSTANCE};
    }
    List<JsonTokenExtractor> executorList = new ArrayList<>();
    String[] tokens = exp.split("\\.");
    for (String token : tokens) {
      token = token.trim();
      List<JsonTokenExtractor> executors = processToken(token);
      if (executors == null) {
        throw new IllegalArgumentException("Invalid expression: " + exp);
      }
      executorList.addAll(executors);
    }
    return executorList.toArray(new JsonTokenExtractor[0]);
  }

  public static AttributeKey buildAttributeKey(Object data, String tagKey) {
    AttributeKey key = null;
    if (data instanceof String) {
      key = AttributeKey.stringKey(tagKey);
    } else if (data instanceof Number) {
      Class<?> clazz = data.getClass();
      if(clazz == double.class || clazz == Double.class || clazz == float.class || clazz == Float.class){
        key = AttributeKey.doubleKey(tagKey);
      }else{
        key = AttributeKey.longKey(tagKey);
      }
    } else if (data instanceof Boolean) {
      key = AttributeKey.booleanKey(tagKey);
    }
    return key;
  }

  // $.[0][1].users[1].name
  private static List<JsonTokenExtractor> processToken(String token) {
    List<JsonTokenExtractor> extractors = new ArrayList<>();
    int idx = -1;
    boolean in = false;
    for (int i = 0; i < token.length(); i++) {
      char c = token.charAt(i);
      if (c == '[') {
        in = true;
        if (idx != -1) {
          String seg = token.substring(idx, i);
          extractors.add(new FieldOrMapTokenExtractor(seg));
        }
        idx = -1;
      } else if (c == ']') {
        if (in) {
          String seg = token.substring(idx, i);
          int index = Integer.parseInt(seg);
          extractors.add(new ArrayTokenExtractor(index));
        }
        idx = -1;
      } else if (idx == -1) {
        idx = i;
      }
      if (i == token.length() - 1 && idx != -1) {
        String seg = token.substring(idx, i + 1);
        extractors.add(new FieldOrMapTokenExtractor(seg));
      }
    }
    return extractors;
  }

}
