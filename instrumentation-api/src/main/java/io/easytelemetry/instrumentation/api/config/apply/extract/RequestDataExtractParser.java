package io.easytelemetry.instrumentation.api.config.apply.extract;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.ETelConfigApplier;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 请求数据采集,生成span的tag
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/30
 */
public class RequestDataExtractParser implements ETelConfigApplier {

  private static final Logger logger = Logger.getLogger(RequestDataExtractParser.class.getName());

  public static final RequestDataExtractParser INSTANCE = new RequestDataExtractParser();

  private static final String CONFIG_KEY = "request_data_extract";

  private RequestDataExtractParser() {
  }

  public void apply(Map<String, Object> configs) {
    Object o = configs.remove(CONFIG_KEY);
    if (o == null) {
      return;
    }
    if (!(o instanceof List)) {
      logger.log(Level.WARNING, "request_data_collect config is not a List");
      return;
    }

    DataExtractingConfig collectingConfig = DataExtractingConfig.parseConfig((List) o);
    if (collectingConfig == null) {
      ETelConfig.setDataExtractingConfigs(DataExtractingEntityComposite.EMPTY);
    } else {
      ETelConfig.setDataExtractingConfigs(new DataExtractingEntityComposite(collectingConfig.getConfigs()));
    }
  }
}
