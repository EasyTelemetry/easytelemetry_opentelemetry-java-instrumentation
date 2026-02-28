package io.easytelemetry.javaagent.tooling.extract.method;

import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.JavaMethodExtractTimingEnum;
import io.easytelemetry.javaagent.tooling.util.CommonVisitor;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.TIMING;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/27
 */
public class ExtractingMethodVisitor extends MethodVisitor {

  private int maxVarIndex;
  private boolean isStatic;
  private Method method;
  private List<DataExtractingEntity> enterParamEntities = new ArrayList<>();
  private List<DataExtractingEntity> exitParamEntities = new ArrayList<>();
  private List<DataExtractingEntity> exitRetEntities = new ArrayList<>();

  protected ExtractingMethodVisitor(int api, MethodVisitor methodVisitor, Method method,
      List<DataExtractingEntity> entityConfigs) {
    super(api, methodVisitor);
    this.method = method;
    this.isStatic = Modifier.isStatic(method.getModifiers());
    for (DataExtractingEntity entity : entityConfigs) {
      switch (entity.getSourceType()) {
        case JAVA_METHOD_PARAMETERS:
          int timing = (int) entity.getJavaMethodMeta().get(TIMING);
          if (timing == JavaMethodExtractTimingEnum.ON_ENTER.getFlag()) {
            enterParamEntities.add(entity);
          } else {
            exitParamEntities.add(entity);
          }
          break;
        case JAVA_METHOD_RETURN_VALUE:
          exitRetEntities.add(entity);
          break;
      }
    }
  }

  @Override
  public void visitCode() {
    super.visitCode();
    doParamExtract(enterParamEntities);
  }

  @Override
  public void visitInsn(int opcode) {
    if (opcode >= IRETURN && opcode <= RETURN) {
      doParamExtract(exitParamEntities);
    }
    if (opcode >= IRETURN && opcode <= ARETURN) {
      doReturnExtract(opcode, exitRetEntities);
    }
    super.visitInsn(opcode);
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    maxVarIndex = Math.max(maxVarIndex, varIndex);
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    super.visitMaxs(maxStack, Math.max(maxLocals, maxVarIndex + 2));
  }

  private void doParamExtract(List<DataExtractingEntity> entityList) {
    for (DataExtractingEntity entity : entityList) {
      int index = entity.getVariableConfigIndex();
      int paramIndex = entity.getExpression().charAt(3) - 48;
      mv.visitLdcInsn(index);

      CommonVisitor.applyPrimitiveTypeWrapping(mv, entity.getVariableType(), isStatic ? paramIndex : paramIndex + 1);

      mv.visitMethodInsn(INVOKESTATIC, ETelExtractMethodSpy.ADVICE_CLASS,
          ETelExtractMethodSpy.extractParamMethod.getName(),
          MethodUtil.getMethodDescriptor(ETelExtractMethodSpy.extractParamMethod), false);
    }
  }

  private void doReturnExtract(int opcode, List<DataExtractingEntity> entityList) {
    int tempVarIndex = maxVarIndex + 2;

    for (int i = 0; i < entityList.size(); i++) {
      DataExtractingEntity entity = entityList.get(i);
      mv.visitInsn(opcode == Opcodes.LRETURN || opcode == Opcodes.DRETURN ? DUP2 : DUP);
      switch (opcode) {
        case IRETURN:
          mv.visitVarInsn(ISTORE, tempVarIndex);
          break;
        case LRETURN:
          mv.visitVarInsn(LSTORE, tempVarIndex);
          break;
        case FRETURN:
          mv.visitVarInsn(FSTORE, tempVarIndex);
          break;
        case DRETURN:
          mv.visitVarInsn(DSTORE, tempVarIndex);
          break;
        default:
          mv.visitVarInsn(ASTORE, tempVarIndex);
          break;
      }
      mv.visitLdcInsn(entity.getVariableConfigIndex());
      CommonVisitor.applyPrimitiveTypeWrapping(mv, method.getReturnType(), tempVarIndex);
      mv.visitMethodInsn(INVOKESTATIC, ETelExtractMethodSpy.ADVICE_CLASS,
          ETelExtractMethodSpy.extractReturnMethod.getName(),
          MethodUtil.getMethodDescriptor(ETelExtractMethodSpy.extractReturnMethod), false);
    }
  }
}
