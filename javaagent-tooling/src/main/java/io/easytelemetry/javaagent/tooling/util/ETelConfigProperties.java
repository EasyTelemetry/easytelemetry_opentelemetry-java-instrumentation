package io.easytelemetry.javaagent.tooling.util;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/4
 */
public class ETelConfigProperties implements ConfigProperties {

  public static final ETelConfigProperties INSTANCE = new ETelConfigProperties();

  @Nullable
  @Override
  public String getString(String name) {
    return null;
  }

  @Nullable
  @Override
  public Boolean getBoolean(String name) {
    return null;
  }

  @Nullable
  @Override
  public Integer getInt(String name) {
    return 0;
  }

  @Nullable
  @Override
  public Long getLong(String name) {
    return 0L;
  }

  @Nullable
  @Override
  public Double getDouble(String name) {
    return 0.0;
  }

  @Nullable
  @Override
  public Duration getDuration(String name) {
    return null;
  }

  @Override
  public List<String> getList(String name) {
    return Collections.emptyList();
  }

  @Override
  public Map<String, String> getMap(String name) {
    return Collections.emptyMap();
  }
}
