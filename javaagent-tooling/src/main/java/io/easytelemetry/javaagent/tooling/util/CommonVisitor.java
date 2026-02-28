package io.easytelemetry.javaagent.tooling.util;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/2/28
 */
public class CommonVisitor {

  public static void applyPrimitiveTypeWrapping(MethodVisitor mv, Class type, int varIndex) {
    if (type == boolean.class) {
      mv.visitVarInsn(ILOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
    } else if (type == byte.class) {
      mv.visitVarInsn(ILOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
    } else if (type == short.class) {
      mv.visitVarInsn(ILOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
    } else if (type == char.class) {
      mv.visitVarInsn(ILOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
    } else if (type == int.class) {
      mv.visitVarInsn(ILOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
    } else if (type == long.class) {
      mv.visitVarInsn(LLOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
    } else if (type == float.class) {
      mv.visitVarInsn(FLOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
    } else if (type == double.class) {
      mv.visitVarInsn(DLOAD, varIndex);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
    } else {
      mv.visitVarInsn(ALOAD, varIndex);
    }
  }

}
