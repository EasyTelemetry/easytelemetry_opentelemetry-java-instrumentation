package io.easytelemetry.javaagent.tooling.bytecode;

import io.easytelemetry.instrumentation.api.utils.Pair;
import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns.LineNumberMapping;
import org.benf.cfr.reader.entities.Method;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.Map.Entry;

public class Decompiler {

  public static Pair<String, NavigableMap<Integer, Integer>> decompile(String classFilePath,
      java.lang.reflect.Method method) {
    return decompile(classFilePath, method, false);
  }

  public static Pair<String, NavigableMap<Integer, Integer>> decompile(String classFilePath,
      java.lang.reflect.Method method, boolean hideUnicode) {
    return decompile(classFilePath, method, hideUnicode, true);
  }

  public static Pair<String, NavigableMap<Integer, Integer>> decompileWithMappings(String classFilePath,
      java.lang.reflect.Method method, boolean hideUnicode, boolean printLineNumber) {
    final StringBuilder sb = new StringBuilder(8192);

    final NavigableMap<Integer, Integer> lineMapping = new TreeMap<Integer, Integer>();

    OutputSinkFactory mySink = new OutputSinkFactory() {
      @Override
      public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
        return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.DECOMPILED_MULTIVER,
            SinkClass.EXCEPTION_MESSAGE, SinkClass.LINE_NUMBER_MAPPING);
      }

      @Override
      public <T> Sink<T> getSink(final SinkType sinkType, final SinkClass sinkClass) {
        return new Sink<T>() {
          @Override
          public void write(T sinkable) {
            if (sinkType == SinkType.PROGRESS) {
              return;
            }
            if (ReflectionUtils.existsField(sinkable, "val$item")) {
              Object item = ReflectionUtils.get(sinkable, "val$item");
              Method cfrMethod = ReflectionUtils.invoke(item, "getMethod");
              String desc = ReflectionUtils.getFieldValues(cfrMethod, "methodPrototype", "originalDescriptor");
              String descriptor = MethodUtil.getMethodDescriptor(method);
              if (!desc.equals(descriptor)) {
                return;
              }
            }
            if (sinkType == SinkType.LINENUMBER) {
              LineNumberMapping mapping = (LineNumberMapping) sinkable;
              NavigableMap<Integer, Integer> classFileMappings = mapping.getClassFileMappings();
              NavigableMap<Integer, Integer> mappings = mapping.getMappings();
              if (classFileMappings != null && mappings != null) {
                for (Entry<Integer, Integer> entry : mappings.entrySet()) {
                  Integer srcLineNumber = classFileMappings.get(entry.getKey());
                  lineMapping.put(entry.getValue(), srcLineNumber);
                }
              }
              return;
            }
            sb.append(sinkable);
          }
        };
      }
    };

    HashMap<String, String> options = new HashMap<String, String>();
    options.put("showversion", "false");
    options.put("hideutf", String.valueOf(hideUnicode));
    options.put("trackbytecodeloc", "true");
    if (method != null) {
      options.put("methodname", method.getName());
    }

    CfrDriver driver = new CfrDriver.Builder().withOptions(options).withOutputSink(mySink).build();
    List<String> toAnalyse = new ArrayList<String>();
    toAnalyse.add(classFilePath);
    driver.analyse(toAnalyse);

    String resultCode = sb.toString();
    if (printLineNumber && !lineMapping.isEmpty()) {
      resultCode = addLineNumber(resultCode, lineMapping);
    }

    return Pair.of(resultCode, lineMapping);
  }

  public static Pair<String, NavigableMap<Integer, Integer>> decompile(String classFilePath,
      java.lang.reflect.Method method, boolean hideUnicode,
      boolean printLineNumber) {
    return decompileWithMappings(classFilePath, method, hideUnicode, printLineNumber);
  }

  private static String addLineNumber(String src, Map<Integer, Integer> lineMapping) {
    int maxLineNumber = 0;
    for (Integer value : lineMapping.values()) {
      if (value != null && value > maxLineNumber) {
        maxLineNumber = value;
      }
    }

    String formatStr = "#%2d: ";
    String emptyStr = "       ";

    StringBuilder sb = new StringBuilder();

    List<String> lines = toLines(src);

    if (maxLineNumber >= 1000) {
      formatStr = "#%4d: ";
      emptyStr = "         ";
    } else if (maxLineNumber >= 100) {
      formatStr = "#%3d: ";
    }

    Pair<Integer, Integer> lineEndpoint = extractTargetMethodLineEndpoint(lines,
        lineMapping.keySet().iterator().next());
    if (lineEndpoint == null) {
      return null;
    }

    for (int i = 0; i < lines.size(); i++) {
      if (i < lineEndpoint.getLeft() || i > lineEndpoint.getRight()) {
        continue;
      }
      String line = lines.get(i);
      Integer srcLineNumber = lineMapping.get(i + 1);
      if (srcLineNumber != null) {
        sb.append(String.format(formatStr, srcLineNumber));
      } else {
        sb.append(emptyStr);
      }
      sb.append(emptyStr);
      sb.append(line).append("\n");
    }
    return sb.toString();
  }

  public static List<String> toLines(String text) {
    List<String> result = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new StringReader(text));
    try {
      String line = reader.readLine();
      while (line != null) {
        result.add(line);
        line = reader.readLine();
      }
    } catch (IOException exc) {

    } finally {
      try {
        reader.close();
      } catch (IOException e) {

      }
    }
    return result;
  }

  private static Pair<Integer, Integer> extractTargetMethodLineEndpoint(List<String> lines, int cl) {
    List<Pair<Integer, Integer>> pairs = new ArrayList<>();
    int start = 0;
    boolean et = false;
    Stack<Character> stack = new Stack<>();
    for (int i = 0; i < lines.size(); i++) {
      String l = lines.get(i).trim();
      if (l.startsWith("/*") || l.startsWith("//") || l.endsWith("*/") || l.startsWith("@")) {
        continue;
      }
      for (int j = 0; j < l.length(); j++) {
        char c = l.charAt(j);
        if (c == '{') {
          et = true;
          stack.push(c);
        }
        if (c == '}') {
          stack.pop();
        }
      }
      if (et && stack.isEmpty()) {
        pairs.add(Pair.of(start, i));
        et = false;
        start = i + 1;
      }
    }
    for (Pair<Integer, Integer> pair : pairs) {
      if (pair.getLeft() <= cl && pair.getRight() >= cl) {
        return pair;
      }
    }
    return null;
  }

}
