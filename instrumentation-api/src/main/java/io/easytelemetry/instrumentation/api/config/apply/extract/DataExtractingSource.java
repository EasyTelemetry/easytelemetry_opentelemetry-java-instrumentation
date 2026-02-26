package io.easytelemetry.instrumentation.api.config.apply.extract;

import java.util.HashMap;
import java.util.Map;

public enum DataExtractingSource {

  SIMPLE_KEY_VALUE(0),
  HTTP_REQUEST_HEADER(1),
  HTTP_RESPONSE_HEADER(2),
  HTTP_REQUEST_PARAMETER(3),
  HTTP_REQUEST_URL(4),
  HTTP_REQUEST_BODY(5),
  HTTP_RESPONSE_BODY(6),
  JAVA_METHOD_PARAMETERS(7),
  JAVA_METHOD_RETURN_VALUE(8),
  JAVA_METHOD_LOCAL_VARIABLE(9);

  private final Integer value;

  private static final Map<Integer, DataExtractingSource> values = new HashMap<>();

  static {
    for (DataExtractingSource value : DataExtractingSource.values()) {
      values.put(value.value, value);
    }
  }

  public int getValue() {
    return value;
  }

  DataExtractingSource(Integer value) {
    this.value = value;
  }

  public static DataExtractingSource valueOf(Integer value) {
    return values.get(value);
  }

}
