package io.easytelemetry.instrumentation.api.config.apply.extract.json;

import java.lang.reflect.Array;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class ArrayTokenExtractor implements JsonTokenExtractor {

  private int index;
  private boolean checked;
  private boolean isArray;

  public ArrayTokenExtractor(int index) {
    this.index = index;
  }

  @Override
  public Object execute(Object target) {
    if (target == null) {
      return null;
    }
    if (!checked) {
      isArray = target.getClass().isArray();
      boolean isList = List.class.isAssignableFrom(target.getClass());
      if (!isArray && !isList) {
        throw new IllegalArgumentException("Target is not an array or List: " + target);
      }
      checked = true;
    }
    if (isArray) {
      if (Array.getLength(target) < index) {
        return null;
      }
      return Array.get(target, index);
    }

    if (((List) target).size() < index) {
      return null;
    }
    return ((List) target).get(index);
  }
}
