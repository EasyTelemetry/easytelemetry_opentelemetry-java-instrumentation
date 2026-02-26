package io.easytelemetry.instrumentation.api.config.apply.extract;

import io.opentelemetry.api.common.AttributeKey;
import java.util.Map;
import java.util.Objects;

public class DataExtractingEntity extends JsonObjectExtractor implements LocalVariableExtractor{

  private DataExtractingSource sourceType;

  private String rootSpanName;

  private String variable;
  private Integer variableIndex;
  private Integer lineNumber;

  private String javaMethodDesc;
  private Map<String, Object> javaMethodMeta;

  private int entityIndex;

  private int hashcode;

  @Override
  public int hashCode() {
    if (hashcode != 0) {
      return hashcode;
    }
    hashcode = Objects.hash(sourceType, javaMethodDesc, expression, tagKey, rootSpanName, variable, lineNumber);
    return hashcode;
  }

  public DataExtractingSource getSourceType() {
    return sourceType;
  }

  public void setSourceType(DataExtractingSource sourceType) {
    this.sourceType = sourceType;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public String getTagKey() {
    return tagKey;
  }

  public void setTagKey(String tagKey) {
    this.tagKey = tagKey;
  }

  public String getRootSpanName() {
    return rootSpanName;
  }

  public void setRootSpanName(String rootSpanName) {
    this.rootSpanName = rootSpanName;
  }

  public Integer getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  public Map<String, Object> getJavaMethodMeta() {
    return javaMethodMeta;
  }

  public String getJavaMethodDesc() {
    return javaMethodDesc;
  }

  public void setJavaMethodDesc(String javaMethodDesc) {
    this.javaMethodDesc = javaMethodDesc;
  }

  public void setJavaMethodMeta(Map<String, Object> javaMethodMeta) {
    this.javaMethodMeta = javaMethodMeta;
  }

  public void setEntityIndex(int entityIndex) {
    this.entityIndex = entityIndex;
  }

  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public Integer getVariableIndex() {
    return variableIndex;
  }

  @Override
  public int getVariableConfigIndex() {
    return entityIndex;
  }

  public void setVariableIndex(Integer variableIndex) {
    this.variableIndex = variableIndex;
  }
}
