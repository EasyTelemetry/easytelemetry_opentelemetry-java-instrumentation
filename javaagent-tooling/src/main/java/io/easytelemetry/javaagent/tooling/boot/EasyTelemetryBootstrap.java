package io.easytelemetry.javaagent.tooling.boot;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.apply.extract.RequestDataExtractParser;
import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.easytelemetry.javaagent.tooling.config.applier.MethodExtractApplier;
import io.easytelemetry.javaagent.tooling.config.applier.SampleRateApplier;
import io.easytelemetry.javaagent.tooling.config.applier.VariableExtractApplier;
import io.easytelemetry.javaagent.tooling.config.listener.EasyTelemetryConfigListener;
import io.easytelemetry.javaagent.tooling.thread.ETelThreadFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 启动类
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/24
 */
public class EasyTelemetryBootstrap {

  public static Map<String, Object> resourceAttributes;

  private static final Logger logger = Logger.getLogger(EasyTelemetryBootstrap.class.getName());

  private static final String ETEL_SERVICE_CONFIG_DIR = "etel.service.config_dir";

  private static final String GLOBAL_CONFIG_FILE_NAME = "easy_telemetry_global.json";

  private EasyTelemetryBootstrap() {
  }

  public static void start(Object resource) {
    ETelThreadFactory.newAgentThread(ETelThreadFactory.AgentThread.CONFIG_FETCHER,
        new Runnable() {
          @Override
          public void run() {


            try {
              String delay = System.getProperty("etel.delay", "180");
              int delayInSec = Integer.parseInt(delay);
              Thread.sleep(delayInSec * 1000);
            } catch (InterruptedException e) {
              // ignore
            }

            resourceAttributes = extractResourceAttributes(resource);
            ETelConfig.init(
                (Long) resourceAttributes.get("process.pid"),
                (String) resourceAttributes.get("host.name"),
                (String) resourceAttributes.get("service.name")
            );

            registerConfigApplier();

            String dir = System.getProperty(ETEL_SERVICE_CONFIG_DIR);
            if (dir == null) {
              String agentPath = EasyTelemetryBootstrap.class.getProtectionDomain().getCodeSource()
                  .getLocation().getPath();
              dir = Paths.get(agentPath).getParent().toFile().getAbsolutePath();
//              logger.log(Level.INFO, "No service config file dir specified. Use agent path as service config dir: " + dir);
            }
            try {
              Path globalPath = Paths.get(dir, GLOBAL_CONFIG_FILE_NAME);
              File globalConfigFile = globalPath.toFile();
              if (!globalConfigFile.exists()) {
                boolean newFile = globalConfigFile.createNewFile();
                if (!newFile) {
                  logger.log(Level.SEVERE,
                      "Can not create easy telemetry config file :"
                          + globalConfigFile.getAbsolutePath());
                  return;
                }
              }

              Path servicePath = Paths.get(dir, ETelConfig.getServiceName() + ".json");
              File serviceConfigFile = servicePath.toFile();
              if (!serviceConfigFile.exists()) {
                boolean newFile = serviceConfigFile.createNewFile();
                if (!newFile) {
                  logger.log(Level.SEVERE,
                      "Can not create easy telemetry config file :"
                          + serviceConfigFile.getAbsolutePath());
                  return;
                }
              }

              // 应用启动时读取配置
              triggerConfigChange(globalPath, servicePath);

              Path configDir = Paths.get(dir);
              WatchService watchService = FileSystems.getDefault().newWatchService();
              configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                  StandardWatchEventKinds.ENTRY_CREATE);
              configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                  StandardWatchEventKinds.ENTRY_CREATE);

              while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                  // 过滤溢出事件
                  if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                  }
                  Path changedFile = (Path) event.context();
                  String fileName = changedFile.getFileName().toString();
                  if (fileName.equals(GLOBAL_CONFIG_FILE_NAME) || fileName.equals(
                      ETelConfig.getServiceName() + ".json")) {
                    triggerConfigChange(globalPath, servicePath);
                  }
                }

                boolean reset = key.reset();
                if (!reset) {
                  logger.log(Level.SEVERE, "Watch key reset failed.");
                  break;
                }
              }

            } catch (InterruptedException e) {
              logger.log(Level.SEVERE, "Watch service config thread interrupted!", e);
            } catch (Throwable e) {
              logger.log(Level.SEVERE, "Watch service config occur exception!", e);
            }
          }
        }).start();
  }

  private static void triggerConfigChange(Path globalPath, Path servicePath) {
    try {
      String globalContent = readConfig(globalPath);
      String serviceContent = readConfig(servicePath);
      EasyTelemetryConfigListener.listenConfig(globalContent, serviceContent);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Read config file failed.", e);
    }
  }

  private static String readConfig(Path filePath) throws IOException {
    List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
    return String.join(System.lineSeparator(), lines);
  }

  private static Map<String, Object> extractResourceAttributes(Object resource) {
    Map<String, Object> resourceAttributes = new HashMap<>();
    Object attrs = ReflectionUtils.invoke(resource, "getAttributes");
    ReflectionUtils.invoke(attrs, "forEach", new BiConsumer<Object, Object>() {

      @Override
      public void accept(Object key, Object value) {
        String keyName = ReflectionUtils.invoke(key, "getKey");
        resourceAttributes.put(keyName, value);
      }
    });
    return resourceAttributes;
  }

  private static void registerConfigApplier() {
    EasyTelemetryConfigListener.registerApplier(RequestDataExtractParser.INSTANCE);
    EasyTelemetryConfigListener.registerApplier(MethodExtractApplier.INSTANCE);
    EasyTelemetryConfigListener.registerApplier(VariableExtractApplier.INSTANCE);
    EasyTelemetryConfigListener.registerApplier(SampleRateApplier.INSTANCE);
  }

}
