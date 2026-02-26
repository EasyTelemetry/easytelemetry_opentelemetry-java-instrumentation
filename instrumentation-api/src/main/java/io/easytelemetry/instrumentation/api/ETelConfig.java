package io.easytelemetry.instrumentation.api;

import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntityComposite;
import java.util.Map;

/**
 * easy-telemetry全局配置
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/22
 */
public abstract class ETelConfig {

  private static boolean logEnabled = true;

  private static long pid = 0;
  private static String hostName = "unknown";
  private static String serviceName = "unknown";

  private static DataExtractingEntityComposite dataExtractingConfigs = DataExtractingEntityComposite.EMPTY;

  private static Map<Integer, Map<String, DataExtractingEntity>> javaMethodExtractingConfigs;
  private static DataExtractingEntity[] indexedVariableExtractingEntities = new DataExtractingEntity[16];
  private static DataExtractingEntity[] indexedMethodExtractingEntities = new DataExtractingEntity[16];

  private ETelConfig() {
  }

  public static void init(long pid, String hostName, String serviceName) {
    ETelConfig.pid = pid;
    ETelConfig.hostName = hostName;
    ETelConfig.serviceName = serviceName;
  }

  public static boolean isLogEnabled() {
    return logEnabled;
  }

  public static long getPid() {
    return pid;
  }

  public static String getHostName() {
    return hostName;
  }

  public static String getServiceName() {
    return serviceName;
  }

  public static void setDataExtractingConfigs(DataExtractingEntityComposite dataExtractingConfigs) {
    ETelConfig.dataExtractingConfigs = dataExtractingConfigs;
  }

  public static DataExtractingEntityComposite getDataExtractingConfigs() {
    return dataExtractingConfigs;
  }

  public static Map<Integer, Map<String, DataExtractingEntity>> getJavaMethodExtractingConfigs() {
    return javaMethodExtractingConfigs;
  }

  public static void setJavaMethodExtractingConfigs(
      Map<Integer, Map<String, DataExtractingEntity>> javaMethodExtractingConfigs) {
    ETelConfig.javaMethodExtractingConfigs = javaMethodExtractingConfigs;
  }

  public static DataExtractingEntity[] getIndexedVariableExtractingEntities() {
    return indexedVariableExtractingEntities;
  }

  public static void setIndexedVariableExtractingEntities(
      DataExtractingEntity[] indexedVariableExtractingEntities) {
    ETelConfig.indexedVariableExtractingEntities = indexedVariableExtractingEntities;
  }

  public static DataExtractingEntity[] getIndexedMethodExtractingEntities() {
    return indexedMethodExtractingEntities;
  }
}
