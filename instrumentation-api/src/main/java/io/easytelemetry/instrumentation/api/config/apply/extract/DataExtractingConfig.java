package io.easytelemetry.instrumentation.api.config.apply.extract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.*;

public class DataExtractingConfig {

  private static final Logger log = Logger.getLogger(DataExtractingConfig.class.getName());

  private final List<DataExtractingEntity> configs;

  public DataExtractingConfig(List<DataExtractingEntity> configs) {
    this.configs = configs;
  }

  public List<DataExtractingEntity> getConfigs() {
    return configs;
  }

  public static DataExtractingConfig parseConfig(List<?> configs) {
    if (configs == null || configs.isEmpty()) {
      return null;
    }
    List<DataExtractingEntity> cfg = new ArrayList<>(configs.size());
    for (Object obj : configs) {
      if (!(obj instanceof Map)) {
        continue;
      }
      Map<String, Object> config = (Map<String, Object>) obj;
      DataExtractingEntity entity = new DataExtractingEntity();
      Object source = config.get("sourceType");
      if (source == null) {
        log.log(Level.SEVERE, "Missing data collect config 'sourceType' !");
        continue;
      }
      if (source instanceof Double) {
        source = ((Double) source).intValue();
      }
      DataExtractingSource sourceValue = DataExtractingSource.valueOf(Integer.valueOf(source.toString()));
      if (sourceValue == null) {
        log.log(Level.SEVERE, "Illegal data collect config source '{}' !", source);
        continue;
      }
      entity.setSourceType(sourceValue);

      String expression = (String) config.get("expression");
      if (sourceValue != DataExtractingSource.HTTP_REQUEST_URL) {
        if (expression == null) {
          log.log(Level.SEVERE, "Missing data collect config 'expression' !");
          continue;
        }
      }
      entity.setExpression(expression);

      Double lineNumber = (Double) config.get("lineNumber");
      if (lineNumber != null) {
        entity.setLineNumber(lineNumber.intValue());
      }

      if (sourceValue == DataExtractingSource.JAVA_METHOD_PARAMETERS
          || sourceValue == DataExtractingSource.JAVA_METHOD_LOCAL_VARIABLE
          || sourceValue == DataExtractingSource.JAVA_METHOD_RETURN_VALUE) {
        String desc = (String) config.get("javaMethodDesc");
        if (desc == null) {
          log.log(Level.SEVERE, "Missing data collect config 'javaMethodDesc' !");
          continue;
        }
        if (!expression.startsWith("$.")) {
          log.log(Level.SEVERE, "'expression' config must start with '$.' !");
          continue;
        }
        entity.setJavaMethodDesc(desc);
        entity.setJavaMethodMeta(parseMethodDesc(desc));
        Double timing = (Double) config.get("timing");
        if (timing != null && timing > 0) {
          JavaMethodExtractTimingEnum timingEnum = JavaMethodExtractTimingEnum.valueOf(timing.intValue());
          if (timingEnum == null) {
            log.log(Level.SEVERE, "Illegal data collect config 'timing' !");
            continue;
          }
          entity.getJavaMethodMeta().put("timing", timingEnum.getFlag());
        } else if (sourceValue == DataExtractingSource.JAVA_METHOD_PARAMETERS) {
          entity.getJavaMethodMeta().put("timing", JavaMethodExtractTimingEnum.ON_ENTER.getFlag());
        }
      }

      if (sourceValue == DataExtractingSource.JAVA_METHOD_LOCAL_VARIABLE) {
        String variable = (String) config.get("variable");
        if (variable == null) {
          log.log(Level.SEVERE, "Missing data collect config 'variable' !");
          continue;
        }
        entity.setVariable(variable);
      }

      String tagKey = (String) config.get("tagKey");
      if (tagKey == null) {
        log.log(Level.SEVERE, "Missing data collect config 'tagKey' !");
        continue;
      }
      entity.setTagKey(tagKey);

      Object rootSpanName = config.get("rootSpanName");
      if (rootSpanName != null && rootSpanName instanceof String) {
        entity.setRootSpanName((String) rootSpanName);
      }

      cfg.add(entity);
    }
    return new DataExtractingConfig(cfg);
  }

  public static Map<String, Object> parseMethodDesc(String methodDesc) {
    if (!methodDesc.contains("(") || !methodDesc.contains(")")) {
      return null;
    }
    String[] split = methodDesc.split("#", 2);
    Map<String, Object> desc = new HashMap<>(4);
    desc.put(DESC, methodDesc);
    desc.put(CLASS_NAME, split[0]);

    String[] ms = split[1].split("\\(", 2);
    desc.put(METHOD_NAME, ms[0]);

    String params = ms[1].substring(0, ms[1].length() - 1).trim();
    String[] paramTypes;
    if (params.trim().length() == 0) {
      paramTypes = new String[0];
    } else {
      paramTypes = params.split(",");
    }
    desc.put(PARAM_TYPES, paramTypes);
    return desc;
  }

}
