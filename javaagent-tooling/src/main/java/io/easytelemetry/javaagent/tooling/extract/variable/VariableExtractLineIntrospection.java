package io.easytelemetry.javaagent.tooling.extract.variable;

import io.easytelemetry.instrumentation.api.config.apply.extract.LocalVariableExtractor;
import io.easytelemetry.instrumentation.api.utils.Pair;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.Label;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/2/3
 */
public class VariableExtractLineIntrospection {

  private Map<String, List<LocalVariableExtractor>> variableEntities;

  private Map<String, Integer> variableSlotIndex = new HashMap<>();
  private Map<String, Pair<Integer, Integer>> variableScopeLineEndpoint = new HashMap<>();

  private int maxLineNumber;
  private Map<Label, Integer> labelStartLine = new HashMap<>();

  private Map<Integer, List<LocalVariableExtractor>> lineEntities;

  private String errorMsg;

  public VariableExtractLineIntrospection(Map<String, List<LocalVariableExtractor>> variableEntities) {
    this.variableEntities = variableEntities;
  }

  final void visitLineNumber(int line, Label label) {
    labelStartLine.putIfAbsent(label, line);
    maxLineNumber = Math.max(maxLineNumber, line);
  }

  final void visitLocalVariable(String name, Label start, Label end, int index) {
    if (errorMsg != null) {
      return;
    }
    if (variableSlotIndex.containsKey(name)) {
      return;
    }
    List<LocalVariableExtractor> entities = variableEntities.get(name);
    if (entities == null) {
      return;
    }
    int maxLine = entities.stream().map(LocalVariableExtractor::getLineNumber).mapToInt(i -> i).max().getAsInt();
    int minLine = entities.stream().map(LocalVariableExtractor::getLineNumber).mapToInt(i -> i).min().getAsInt();

    Integer startLine = labelStartLine.get(start);
    Integer endLine = labelStartLine.get(end);
    if (endLine == null) {
      endLine = maxLineNumber + 1;
      labelStartLine.put(end, endLine);
    }

    Collection<Integer> values = labelStartLine.values();
    for (LocalVariableExtractor entity : entities) {
      Integer lineNumber = entity.getLineNumber();
      if(!values.contains(lineNumber)){
        errorMsg = "Variable " + name + " is not in the scope of line " + lineNumber;
        return;
      }
    }

    // 变量作用域不匹配
    if (maxLine < startLine || minLine > endLine) {
      return;
    }

    if (minLine < startLine || maxLine > endLine) {
      errorMsg = "Variable " + name + " is not in the scope of line " + minLine + " to " + maxLine;
      return;
    }

    variableSlotIndex.put(name, index);
    variableScopeLineEndpoint.put(name, Pair.of(startLine, endLine));

    for (LocalVariableExtractor entity : entities) {
      entity.setVariableIndex(index);
    }
  }

  public final void refresh() {
    lineEntities = variableEntities.values().stream()
        .flatMap(new Function<List<LocalVariableExtractor>, Stream<LocalVariableExtractor>>() {
          @Override
          public Stream<LocalVariableExtractor> apply(List<LocalVariableExtractor> entityList) {
            return entityList.stream();
          }
        }).collect(Collectors.groupingBy(LocalVariableExtractor::getLineNumber));
  }

  public final List<LocalVariableExtractor> getLineEntities(int line) {
    return lineEntities.get(line);
  }

  public String getErrorMsg() {
    if (errorMsg != null) {
      return errorMsg;
    }
    for (Map.Entry<String, List<LocalVariableExtractor>> entry : variableEntities.entrySet()) {
      for (LocalVariableExtractor entity : entry.getValue()) {
        if (entity.getVariableIndex() == null) {
          return "Variable " + entry.getKey() + " is not in the scope of line " + entity.getLineNumber();
        }
      }
    }
    return null;
  }
}
