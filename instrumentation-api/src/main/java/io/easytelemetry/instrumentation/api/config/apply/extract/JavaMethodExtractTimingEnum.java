package io.easytelemetry.instrumentation.api.config.apply.extract;

import java.util.HashMap;
import java.util.Map;

public enum JavaMethodExtractTimingEnum {
  ON_ENTER(1),
  ON_EXIT(2);

  private static final Map<Integer,JavaMethodExtractTimingEnum> values = new HashMap<>();

  static {
    for (JavaMethodExtractTimingEnum value : JavaMethodExtractTimingEnum.values()) {
      values.put(value.flag, value);
    }
  }

  private int flag;

  JavaMethodExtractTimingEnum(int flag) {
    this.flag = flag;
  }

  public int getFlag() {
    return flag;
  }

  public static JavaMethodExtractTimingEnum valueOf(int flag) {
    return values.get(flag);
  }

}
