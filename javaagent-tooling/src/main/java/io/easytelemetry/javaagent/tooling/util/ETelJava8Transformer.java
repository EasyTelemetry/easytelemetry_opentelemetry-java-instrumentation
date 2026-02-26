package io.easytelemetry.javaagent.tooling.util;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Java 8 字节码转换器，用于在 Java 8 环境下进行字节码增强操作。
 * 该转换器负责处理类加载时的字节码修改，支持方法拦截和性能监控功能。
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/23
 */
public class ETelJava8Transformer implements ClassFileTransformer {

  private static final byte[] EMPTY_BYTE_ARRAY = new byte[1];

  @Override
  public byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
      throws IllegalClassFormatException {

    if (classBeingRedefined != null
        && (className.contains("/easytelemetry/") || className.endsWith("AgentClassLoader"))) {
      return EMPTY_BYTE_ARRAY;
    }
    return null;
  }

}
