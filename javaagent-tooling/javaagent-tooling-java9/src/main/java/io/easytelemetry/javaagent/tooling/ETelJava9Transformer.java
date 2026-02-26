package io.easytelemetry.javaagent.tooling;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/12/23
 */
public class ETelJava9Transformer implements ClassFileTransformer {

  private static final byte[] EMPTY_BYTE_ARRAY = new byte[1];

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
