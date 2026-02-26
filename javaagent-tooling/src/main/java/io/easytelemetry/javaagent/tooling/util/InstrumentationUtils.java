package io.easytelemetry.javaagent.tooling.util;


import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstrumentationUtils {

  private static final Logger logger = Logger.getLogger(InstrumentationUtils.class.getName());

  private static Class[] loadedClasses;

  private InstrumentationUtils() {
  }

  public static boolean retransformClasses(ClassFileTransformer transformer, Class... classes) {
    Instrumentation inst = InstrumentationHolder.getInstrumentation();
    try {
      inst.addTransformer(transformer, true);
      inst.retransformClasses(classes);
      return true;
    } catch (UnmodifiableClassException e) {
      logger.log(Level.SEVERE, "RetransformClasses class error", e);
      inst.removeTransformer(transformer);
      return false;
    } finally {
      inst.removeTransformer(transformer);
    }
  }

  public static void retransformClasses(ClassFileTransformer transformer, List<Class<?>> classes) {
    Instrumentation inst = InstrumentationHolder.getInstrumentation();
    try {
      inst.addTransformer(transformer, true);
      for (Class<?> clazz : classes) {
        if (isLambdaClass(clazz)) {
          continue;
        }
        try {
          inst.retransformClasses(clazz);
        } catch (Throwable e) {
          logger.log(Level.SEVERE, "RetransformClasses class error, class:" + clazz.getName(), e);
        }
      }
    } finally {
      inst.removeTransformer(transformer);
    }
  }

  public static boolean isLambdaClass(Class<?> clazz) {
    return clazz.getName().contains("$$Lambda$");
  }

  public static Class findClass(String className) {
    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
    if (instrumentation == null) {
      return null;
    }
    for (Class clazz : instrumentation.getAllLoadedClasses()) {
      if (className.equals(clazz.getName())) {
        return clazz;
      }
    }
    return null;
  }

  public static List<Class> findClass(String... classNames) {
    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
    if (instrumentation == null) {
      return null;
    }
    Set<String> nameSet = new HashSet<>(classNames.length);
    for (String className : classNames) {
      nameSet.add(className);
    }

    List<Class> classes = new ArrayList<>();
    for (Class clazz : instrumentation.getAllLoadedClasses()) {
      if (nameSet.contains(clazz.getName())) {
        classes.add(clazz);
      }
    }
    return classes;
  }

  public static Class[] getLoadedClasses() {
    if(loadedClasses == null){
      loadedClasses = InstrumentationHolder.getInstrumentation().getAllLoadedClasses();
    }
    return loadedClasses;
  }

  public static void clearClasses(){
    loadedClasses = null;
  }
}
