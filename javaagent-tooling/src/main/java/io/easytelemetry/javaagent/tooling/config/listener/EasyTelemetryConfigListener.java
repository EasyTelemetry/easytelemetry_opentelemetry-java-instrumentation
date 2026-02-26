package io.easytelemetry.javaagent.tooling.config.listener;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.easytelemetry.instrumentation.api.config.ETelConfigApplier;
import io.easytelemetry.javaagent.tooling.util.InstrumentationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置监听器
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/30
 */
public class EasyTelemetryConfigListener {

  private static final Logger logger = Logger.getLogger(EasyTelemetryConfigListener.class.getName());

  private static final List<ETelConfigApplier> APPLIERS = new ArrayList<>();

  private static final JsonAdapter<Map<String, Object>> ADAPTER =
      new Moshi.Builder().build().adapter(Types.newParameterizedType(Map.class, String.class, Object.class));

  public static void listenConfig(String globalConfig, String serviceConfig) {
    try {
      Map<String, Object> config;
      if (globalConfig != null && !globalConfig.isEmpty()) {
        config = ADAPTER.fromJson(globalConfig);
      } else {
        config = new HashMap<>();
      }
      if (serviceConfig != null && !serviceConfig.isEmpty()) {
        Map<String, Object> serviceCfgMap = ADAPTER.fromJson(serviceConfig);
        config.putAll(serviceCfgMap);
      }

      for (ETelConfigApplier applier : APPLIERS) {
        try {
          applier.apply(config);
        } catch (Throwable e) {
          logger.log(Level.SEVERE, "Apply easy telemetry config failed!", e);
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Apply easy telemetry config failed!", e);
    }finally {
      InstrumentationUtils.clearClasses();
    }
  }

  public static void registerApplier(ETelConfigApplier applier) {
    APPLIERS.add(applier);
  }

}
