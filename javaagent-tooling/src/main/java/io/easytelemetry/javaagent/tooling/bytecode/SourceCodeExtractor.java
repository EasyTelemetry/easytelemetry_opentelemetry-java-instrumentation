package io.easytelemetry.javaagent.tooling.bytecode;

import io.easytelemetry.javaagent.tooling.util.InstrumentationUtils;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceCodeExtractor {

  private static final Logger logger = Logger.getLogger(SourceCodeExtractor.class.getName());

  private static final String CLASSES = "classes";

  private static File AGENT_DIR;
  private static File CLASS_DIR;

  private static synchronized void init(Class agentClass) {
    if (AGENT_DIR == null) {
      AGENT_DIR = new File(agentClass.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
      CLASS_DIR = new File(AGENT_DIR, CLASSES);
    }
    if(CLASS_DIR.exists()){
      deleteDir(CLASS_DIR);
    }
  }

  public static String extractSourceCode(Class clazz) {
    try {
      String classFilePath = getClassFilePath(clazz);
      if (!new File(classFilePath).exists()) {
        doSaveSourceCode(clazz);
      }
      return Decompiler.decompile(classFilePath, null).getLeft();
    } catch (Exception e) {
      logger.log(Level.SEVERE, String.format("Failed extract method %s source code!", clazz.getName()));
      return null;
    }
  }

  public static String extractSourceCode(Method method) {
    try {
      String fileName = getClassFilePath(method.getDeclaringClass());
      if (!new File(fileName).exists()) {
        doSaveSourceCode(method.getDeclaringClass());
      }
      return Decompiler.decompile(fileName, method).getLeft();
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          String.format("Failed extract method %s#%s source code!", method.getDeclaringClass().getName(),
              method.getName()));
      return null;
    }
  }

  public static List<Integer> extractAvailableLineNumbers(Method method) {
    try {
      String fileName = getClassFilePath(method.getDeclaringClass());
      if (!new File(fileName).exists()) {
        doSaveSourceCode(method.getDeclaringClass());
      }
      Collection<Integer> lines = Decompiler.decompile(fileName, method).getRight().values();
      ArrayList<Integer> lineNumbers = new ArrayList<>(lines);
      Collections.sort(lineNumbers);
      return lineNumbers;
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          String.format("Failed extract method %s line numbers!", MethodUtil.getMethodDescriptor(method)));
      return null;
    }
  }

  private static void doSaveSourceCode(Class clazz) {
    InstrumentationUtils.retransformClasses(new ClassFileTransformer() {
      @Override
      public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
          ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (classBeingRedefined == clazz) {
          SourceCodeExtractor.saveSourceCode(this.getClass(), className, classfileBuffer);
        }
        return classfileBuffer;
      }
    }, clazz);
  }

  public static boolean saveSourceCode(Class operator, String className, byte[] classfileBuffer) {
    if (AGENT_DIR == null) {
      init(operator);
    }
    if (className == null) {
      logger.log(Level.SEVERE, "Can`t save source code with null name!");
      return false;
    }
    if (!CLASS_DIR.exists()) {
      CLASS_DIR.mkdirs();
    }
    try {
      File saveFile = new File(CLASS_DIR, className + ".class");
      if (saveFile.exists()) {
        byte[] bytes = Files.readAllBytes(saveFile.toPath());
        if (bytes.length == classfileBuffer.length) {
          return true;
        }
        saveFile.delete();
      }

      File dir = new File(CLASS_DIR, className.substring(0, className.lastIndexOf("/")));
      if (!dir.exists()) {
        dir.mkdirs();
      }

      boolean created = saveFile.createNewFile();
      if (!created) {
        logger.log(Level.SEVERE, "Failed to create new source file for class:", className);
        return false;
      }
      Files.write(saveFile.toPath(), classfileBuffer);
      return true;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Save class source code occur exception!", e);
    }
    return false;
  }

  private static String getClassFilePath(Class clazz) {
    if (AGENT_DIR == null) {
      init(SourceCodeExtractor.class);
    }
    return CLASS_DIR.getAbsolutePath() + File.separator + clazz.getName().replace(".", "/") + ".class";
  }

  /**
   * 递归删除目录及所有子文件/子目录
   *
   * @param dir 要删除的目录
   * @return 删除成功返回 true，失败返回 false
   */
  private static boolean deleteDir(File dir) {
    // 若不是目录，直接删除文件
    if (!dir.isDirectory()) {
      return dir.delete();
    }

    // 递归删除所有子文件和子目录
    File[] files = dir.listFiles();
    if (files != null) { // 避免 null 指针（目录不存在或权限不足）
      for (File file : files) {
        boolean deleted = deleteDir(file);
        if (!deleted) {
          System.err.println("删除文件失败：" + file.getAbsolutePath());
          return false; // 有一个文件删除失败，整体返回失败
        }
      }
    }
    // 所有子文件/目录删除后，删除当前空目录
    return dir.delete();
  }

  public static void destroy() {
    if (CLASS_DIR != null) {
      deleteDir(CLASS_DIR);
    }
  }
}
