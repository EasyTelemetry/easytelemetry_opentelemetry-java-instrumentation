package io.easytelemetry.instrumentation.api.config.apply.extract;

public interface LocalVariableExtractor {

  String getVariable();

  Integer getLineNumber();

  void setVariableIndex(Integer variableIndex);

  Integer getVariableIndex();

  int getVariableConfigIndex();

  void serVariableType(Class type);

  Class getVariableType();

}
