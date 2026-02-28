package io.easytelemetry.javaagent.tooling.extract.variable;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.LocalVariableExtractor;
import io.easytelemetry.javaagent.tooling.util.CommonVisitor;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import java.util.List;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/23
 */
public class VariableExtractVisitor extends MethodVisitor {

  private VariableExtractLineIntrospection lineIntrospection;

  private int latestLine;

  VariableExtractVisitor(int api, MethodVisitor methodVisitor, VariableExtractLineIntrospection lineIntrospection) {
    super(api, methodVisitor);
    this.lineIntrospection = lineIntrospection;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    List<LocalVariableExtractor> lineEntities = lineIntrospection.getLineEntities(line);
    if (line > latestLine && lineEntities != null && !lineEntities.isEmpty()) {
      for (LocalVariableExtractor extractor : lineEntities) {
        DataExtractingEntity entity = (DataExtractingEntity) extractor;
        mv.visitLdcInsn(entity.getVariableConfigIndex());
        CommonVisitor.applyPrimitiveTypeWrapping(mv, entity.getVariableType(), extractor.getVariableIndex());
        mv.visitMethodInsn(INVOKESTATIC, ETelExtractVariableSpy.ADVICE_CLASS,
            ETelExtractVariableSpy.extractVariableMethod.getName(),
            MethodUtil.getMethodDescriptor(ETelExtractVariableSpy.extractVariableMethod), false);
      }
    }
    super.visitLineNumber(line, start);
    latestLine = Math.max(latestLine, line);
  }
}
