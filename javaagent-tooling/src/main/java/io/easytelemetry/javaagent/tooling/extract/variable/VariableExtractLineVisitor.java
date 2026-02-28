package io.easytelemetry.javaagent.tooling.extract.variable;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/2/3
 */
public class VariableExtractLineVisitor extends MethodVisitor {

  private VariableExtractLineIntrospection lineIntrospection;

  public VariableExtractLineVisitor(int api, MethodVisitor methodVisitor,
      VariableExtractLineIntrospection lineIntrospection) {
    super(api, methodVisitor);
    this.lineIntrospection = lineIntrospection;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    lineIntrospection.visitLineNumber(line, start);
  }

  @Override
  public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    super.visitLocalVariable(name, descriptor, signature, start, end, index);
    lineIntrospection.visitLocalVariable(name, descriptor, start, end, index);
  }
}
