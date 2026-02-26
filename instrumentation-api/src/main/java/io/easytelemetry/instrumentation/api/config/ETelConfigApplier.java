package io.easytelemetry.instrumentation.api.config;

import java.util.Map;

public interface ETelConfigApplier {

  String DESC = "desc";
  String CLASS = "class";
  String METHOD = "method";
  String CLASS_NAME = "className";
  String PARAM_TYPES = "paramTypes";
  String METHOD_NAME = "methodName";
  String STATIC_FLAG = "static";
  String HASHCODE = "hashcode";
  String TIMING = "timing";

  String ROOT_SPAN_NAME = "rootSpanName";

  // trace
  String JAVA_METHOD_DESC = "javaMethodDesc";
  String TRACE_TAG_KEY = "traceTagKey";
  String TRIGGER_OPTIMIZE_TIMES = "triggerOptimizeTimes";
  String TRIGGER_OPTIMIZE_RATIO_THRESHOLD = "triggerOptimizeRatioThreshold";

  //dynamic_config
  String TRACE = "trace";
  String TAGS = "tags";

  void apply(Map<String, Object> config) throws Throwable;
}
