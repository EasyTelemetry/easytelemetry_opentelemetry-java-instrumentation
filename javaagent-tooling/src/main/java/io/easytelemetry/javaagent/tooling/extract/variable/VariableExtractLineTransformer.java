package io.easytelemetry.javaagent.tooling.extract.variable;

import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import org.objectweb.asm.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/2/3
 */
public class VariableExtractLineTransformer implements ClassFileTransformer {

  private Method targetMethod;
  private Class targetClass;
  private String methodDesc;
  private VariableExtractLineIntrospection lineIntrospection;

  public VariableExtractLineTransformer(Method targetMethod, String methodDesc,
      VariableExtractLineIntrospection lineIntrospection) {
    this.targetMethod = targetMethod;
    this.targetClass = targetMethod.getDeclaringClass();
    this.methodDesc = methodDesc;
    this.lineIntrospection = lineIntrospection;
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
                      return method == null ? mv : new VariableExtractLineVisitor(Opcodes.ASM7, mv, lineIntrospection);
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
        if (method == targetMethod) {
          return method;
        } else {
          return null;
        }
      }
    }
    return null;
  }

}
