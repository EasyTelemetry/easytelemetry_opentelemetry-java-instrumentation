package io.easytelemetry.instrumentation.api.config.apply.extract.json;

import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import java.lang.reflect.Type;
import java.util.Map;
import java.lang.reflect.Field;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class FieldOrMapTokenExtractor implements JsonTokenExtractor {

  private String token;

  private Type tokenType;
  private Object tokenValue;

  private Boolean fieldExists;
  private Field field;

  public FieldOrMapTokenExtractor(String token) {
    this.token = token;
  }

  @Override
  public Object execute(Object target) {
    if (target == null) {
      return null;
    }
    if (field != null) {
      return ReflectionUtils.getField(field, target);
    }
    if (tokenValue != null) {
      return ((Map) target).get(tokenValue);
    }

    if (!Map.class.isAssignableFrom(target.getClass())) {
      if (fieldExists == null) {
        fieldExists = ReflectionUtils.existsField(target, token);
        if (fieldExists) {
          field = ReflectionUtils.findField(target, token);
          field.setAccessible(true);
        }
      }
      if (fieldExists) {
        return ReflectionUtils.getField(field, target);
      } else {
        throw new IllegalArgumentException("field " + token + " not exists");
      }
    }
    if (tokenType == null) {
      Map map = (Map) target;
      if (map.isEmpty()) {
        return null;
      }
      tokenType = map.keySet().iterator().next().getClass();
      if (tokenType == String.class) {
        tokenValue = token.toString();
      } else if (tokenType == Integer.class) {
        tokenValue = Integer.parseInt(token.toString());
      } else if (tokenType == Long.class) {
        tokenValue = Long.parseLong(token.toString());
      } else if (tokenType == Double.class) {
        tokenValue = Double.parseDouble(token.toString());
      } else if (tokenType == Float.class) {
        tokenValue = Float.parseFloat(token.toString());
      } else if (tokenType == Boolean.class) {
        tokenValue = Boolean.parseBoolean(token.toString());
      } else {
        throw new IllegalArgumentException("Unsupported token type: " + tokenType);
      }
    }
    return ((Map) target).get(tokenValue);
  }
}
