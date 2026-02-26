package io.easytelemetry.javaagent.tooling.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class IpUtil {

  private static volatile String containerIp;
  private static volatile String containerHostName;
  private static final String HOST_IP_ENV = System.getenv("DF_HOST_IP");
  private static volatile String defaultIp = HOST_IP_ENV;
  private static volatile String defaultHostName;
  public static boolean runningInContainer = isRunningInsideDocker();

  //  private static final String inet_file_cfg_directory = "/etc/sysconfig/network-scripts/";
  private static final String inet_file_cfg_directory = "/opt/sysconfig/network-scripts/";
  private static final String hostname_file_0 = "/opt/hostname";
  private static final String hostname_file_1 = "/etc/hostname";
  private static final String JVM_RUNNING_IN_DOCKER = "jvm_running_in_docker";

  private static final String SERVICE_IP_CONFIG_KEY = "DATABUFF_SERVICE_IP";
  private static final String SERVICE_IP_MAPPINGS_KEY = "databuff.service.ip.mappings";

  public static void updateDefaultIp(String hostIp) {
    if (HOST_IP_ENV == null && !runningInContainer && hostIp != null && !hostIp.equals(defaultIp)) {
      defaultIp = hostIp;
    }
  }

  public static void updateDefaultHostName(String hostName) {
    if (!runningInContainer && hostName != null && !hostName.equals(defaultHostName)) {
      defaultHostName = hostName;
    }
  }

  public static String getPhysicalHostName() {
    return runningInContainer ? getHostnameInContainer() : getHostNameByDefault();
  }

  public static String getPhysicalIp() {
    if (runningInContainer) {
      if (containerIp == null) {
        String configIp = getIpByConfig();
        containerIp = configIp != null ? configIp : getIpInContainer();
      }
      return containerIp;
    } else {
      if (defaultIp == null) {
        String configIp = getIpByConfig();
        defaultIp = configIp != null ? configIp : getIpByDefault();
      }
      return defaultIp;
    }
  }

  private static String getIpByConfig() {
    String ipMappings = System.getProperty(SERVICE_IP_CONFIG_KEY, System.getenv(SERVICE_IP_CONFIG_KEY));
    if (ipMappings != null) {
      return ipMappings;
    }
    ipMappings = System.getProperty(SERVICE_IP_MAPPINGS_KEY, System.getenv(SERVICE_IP_MAPPINGS_KEY));
    if (ipMappings == null) {
      return null;
    }
    String foundIp = runningInContainer ? getIpInContainer() : getIpByDefault();
    String[] split = ipMappings.trim().split(",");
    for (String mapping : split) {
      if (!mapping.contains(foundIp)) {
        continue;
      }
      String[] splits = mapping.split(">", 2);
      if (mapping.equals(split[0])) {
        return splits[1];
      }
    }
    return null;
  }

  public static String getIpByDefault() {
    if (defaultIp != null) {
      return defaultIp;
    }
    String ip = null;
    try {
      InetAddress addr = InetAddress.getLocalHost();
      ip = addr.getHostAddress();
    } catch (Throwable e) {
    }
    if (ip == null || "127.0.0.1".equals(ip)) {
      ip = getIpByNetworkInterface();
    }
    if (ip == null || "127.0.0.1".equals(ip)) {
      ip = getIpByNetworkInterfaceOptional();
    }
    if (ip == null) {
      ip = "unknown";
    }
    defaultIp = ip;
    return ip;
  }

  private static String getIpByNetworkInterface() {
    Map<NetworkInterface, InetAddress> addresses = new HashMap<>();
    try {
      Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
      while (n.hasMoreElements()) {
        NetworkInterface netInterface = n.nextElement();
        if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp()) {
          Enumeration<InetAddress> a = netInterface.getInetAddresses();
          while (a.hasMoreElements()) {
            InetAddress inetAddress = a.nextElement();
            if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
              addresses.putIfAbsent(netInterface, inetAddress);
            }
          }
        }
      }
    } catch (SocketException e) {
      return null;
    }
    for (Map.Entry<NetworkInterface, InetAddress> entry : addresses.entrySet()) {
      String name = entry.getKey().getDisplayName();
      if (name.equals("eth0") || name.startsWith("en")) {
        return entry.getValue().getHostAddress();
      }
    }
    if (!addresses.isEmpty()) {
      return addresses.entrySet().iterator().next().getValue().getHostAddress();
    }
    return null;
  }

  private static String getIpByNetworkInterfaceOptional() {
    try {
      Field field = NetworkInterface.class.getDeclaredField("bindings");
      field.setAccessible(true);
      Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
      while (n.hasMoreElements()) {
        NetworkInterface netInterface = n.nextElement();
        String name = netInterface.getDisplayName();
        if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp() && ((name.equals("eth0") || name.startsWith("en")))) {
          InterfaceAddress[] addresses = (InterfaceAddress[]) field.get(netInterface);
          if (addresses != null && addresses.length > 0) {
            for (InterfaceAddress address : addresses) {
              InetAddress inetAddress = address.getAddress();
              if (inetAddress != null) {
                String hostName = inetAddress.getHostName();
                if (hostName != null && !"127.0.0.1".equals(hostName) && hostName.split("\\.").length == 4) {
                  return hostName;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private static String getHostnameByFS() {
    File file = new File("/proc/sys/kernel/hostname");
    if (!file.exists()) {
      return null;
    }
    try {
      List<String> strings = Files.readAllLines(file.toPath());
      for (String line : strings) {
        String trimLine = line.trim();
        if (!trimLine.startsWith("#") && !trimLine.startsWith("//")) {
          return trimLine;
        }
      }
    } catch (IOException e) {
    }
    return null;
  }


  public static String getHostNameByDefault() {
    if (defaultHostName == null) {
      try {
        InetAddress addr = InetAddress.getLocalHost();
        defaultHostName = addr.getHostName();
      } catch (Throwable e) {
      }
      if (defaultHostName == null || "localhost".equals(defaultHostName)) {
        defaultHostName = getHostnameByFS();
      }
      if (defaultHostName == null) {
        defaultHostName = "unknown";
      }
    }
    return defaultHostName;
  }

  private static String getIpInContainer() {
    if (containerIp != null) {
      return containerIp;
    }
    String ip = "unknown";
    Path direc = Paths.get(inet_file_cfg_directory);
    if (!direc.toFile().exists()) {
      return null;
    }
    for (File file : direc.toFile().listFiles()) {
      // 找到ifcfg-ens192文件
      if (file.getName().startsWith("ifcfg-ens") && !file.getName().contains(".")) {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(file)) {
          properties.load(inputStream);
        } catch (IOException e) {
        }
        ip = properties.getProperty("IPADDR").trim();
        break;
      }
    }
    if (ip.startsWith("\"") && ip.endsWith("\"")) {
      ip = ip.substring(1, ip.length() - 1);
    }
    return ip;
  }

  private static String getHostnameInContainer() {
    if (containerHostName != null) {
      return containerHostName;
    }
    containerHostName = "unknown";
    Path hostFile = Paths.get(hostname_file_0);
    if (!hostFile.toFile().exists()) {
      hostFile = Paths.get(hostname_file_1);
      if (!hostFile.toFile().exists()) {
        return containerHostName;
      }
    }
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(hostFile.toFile()));
      String line;
      while ((line = reader.readLine()) != null && !line.trim().startsWith("#")) {
        containerHostName = line.trim();
        break;
      }
    } catch (Exception e) {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (Exception ex) {
      }
    }
    return containerHostName;
  }

  private static boolean isRunningInsideDocker() {
    String agentUrl = System.getProperty("df.trace.agent.url");
    if (agentUrl != null && agentUrl.startsWith("unix")) {
      return true;
    }
    if (System.getProperty(JVM_RUNNING_IN_DOCKER) != null) {
      return true;
    }
    File file = new File("/proc/1/cgroup");
    if (!file.exists()) {
      return false;
    }
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("/docker") || line.contains("/cke") || line.contains("/kubepods")) {
          return true;
        }
      }
    } catch (Exception e) {
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {

        }
      }
    }
    return false;
  }
}

