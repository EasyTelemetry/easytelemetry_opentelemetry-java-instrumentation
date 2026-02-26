package io.easytelemetry.javaagent.tooling.extract.method;

import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/26
 */
public class ExtractingMethodTransformer implements ClassFileTransformer {

  private Class targetClass;
  private Map<Method, List<DataExtractingEntity>> methodExtractingEntities;

  public ExtractingMethodTransformer(Map<Method, List<DataExtractingEntity>> methodExtractingEntities) {
    this.targetClass = methodExtractingEntities.keySet().iterator().next().getDeclaringClass();
    this.methodExtractingEntities = methodExtractingEntities;
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    ClassReader reader = new ClassReader(classfileBuffer);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
    reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                      MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                      Method method = findMethod(access, name, descriptor);
                      return method == null ? mv
                          : new ExtractingMethodVisitor(Opcodes.ASM7, mv, method, methodExtractingEntities.get(method));
                    }
                  },
        0);
    return writer.toByteArray();
  }


  public Method findMethod(int access, String name, String descriptor) {
    for (Method method : ReflectionUtils.getDeclaredMethods(targetClass)) {
      int modifiers = method.getModifiers();
      String methodName = method.getName();
      String methodDescriptor = MethodUtil.getMethodDescriptor(method);
      if (modifiers == access && methodName.equals(name) && descriptor.equals(methodDescriptor)) {
        if (methodExtractingEntities.containsKey(method)) {
          return method;
        } else {
          return null;
        }
      }
    }
    return null;
  }
}
