package io.easytelemetry.javaagent.tooling.util;


import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.CLASS;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.CLASS_NAME;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.DESC;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.HASHCODE;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.METHOD;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.METHOD_NAME;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.PARAM_TYPES;
import static io.easytelemetry.instrumentation.api.config.ETelConfigApplier.STATIC_FLAG;

import io.easytelemetry.instrumentation.api.utils.ReflectionUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MethodUtil {

  private static final Logger logger = Logger.getLogger(MethodUtil.class.getName());

  // methodDesc >> method/constructor#hashcode
  private static final Map<Object, Method> METHOD_DESC_CHECKED = new HashMap<Object, Method>();

  public static boolean hasCheckedMethod(String methodDesc) {
    return METHOD_DESC_CHECKED.containsKey(methodDesc);
  }

  public static Method getMethod(String methodDesc) {
    return METHOD_DESC_CHECKED.get(methodDesc);
  }

  public static String getMethodDescriptor(final Method method) {
    return getMethodDescriptor("", method);
  }

  public static String getMethodDescriptor(String prefix, final Method method) {
    StringBuilder stringBuilder = new StringBuilder(prefix);
    stringBuilder.append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    stringBuilder.append(')');
    appendDescriptor(method.getReturnType(), stringBuilder);
    return stringBuilder.toString();
  }

  public static String getFullMethodDescriptor(final Method method) {
    StringBuilder sb = new StringBuilder();
    String className = method.getDeclaringClass().getName();
    sb.append(className).append(".");
    String name = method.getName();
    sb.append(name);
    return getMethodDescriptor(sb.toString(), method);
  }

  public static boolean returnPrimitiveWrapType(Method method) {
    String descriptor = getMethodDescriptor(method);
    return descriptor.endsWith("java/lang/Integer;") || descriptor.endsWith("java/lang/Boolean;")
        || descriptor.endsWith("java/lang/Byte;") || descriptor.endsWith("java/lang/Character;")
        || descriptor.endsWith("java/lang/Short;") || descriptor.endsWith("java/lang/Double;") || descriptor.endsWith(
        "java/lang/Float;") || descriptor.endsWith("java/lang/Long;");
  }

  private static void appendDescriptor(final Class<?> clazz, final StringBuilder stringBuilder) {
    Class<?> currentClass = clazz;
    while (currentClass.isArray()) {
      stringBuilder.append('[');
      currentClass = currentClass.getComponentType();
    }
    if (currentClass.isPrimitive()) {
      char descriptor;
      if (currentClass == Integer.TYPE) {
        descriptor = 'I';
      } else if (currentClass == Void.TYPE) {
        descriptor = 'V';
      } else if (currentClass == Boolean.TYPE) {
        descriptor = 'Z';
      } else if (currentClass == Byte.TYPE) {
        descriptor = 'B';
      } else if (currentClass == Character.TYPE) {
        descriptor = 'C';
      } else if (currentClass == Short.TYPE) {
        descriptor = 'S';
      } else if (currentClass == Double.TYPE) {
        descriptor = 'D';
      } else if (currentClass == Float.TYPE) {
        descriptor = 'F';
      } else if (currentClass == Long.TYPE) {
        descriptor = 'J';
      } else {
        throw new AssertionError();
      }
      stringBuilder.append(descriptor);
    } else {
      stringBuilder.append('L').append(getInternalName(currentClass)).append(';');
    }
  }

  private static String getInternalName(final Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  public static Method findTransformTargetMethod(Object target, String field, String methodName, String desc) {
    try {
      Object element = ReflectionUtils.get(target, field);
      if (Proxy.isProxyClass(element.getClass())) {
        logger.log(Level.SEVERE, "Can`t find target Method for JDK Proxy class :", element.getClass().getName());
        return null;
      }
      Class targetClass = element.getClass();
      if (targetClass.getName().contains("$$EnhancerBySpringCGLIB$$")) {
        targetClass = targetClass.getSuperclass();
      }
      List<Method> methods = ReflectionUtils.findMethodIgnoreParamTypes(targetClass, methodName);
      for (Method method : methods) {
        if (desc.equals(MethodUtil.getMethodDescriptor("", method))) {
          return method;
        }
      }
      logger.log(Level.SEVERE,
          String.format("Failed find target method %s with desc %s from class %s", methodName, desc,
              targetClass.getName()));
      return null;
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          String.format("Find transform target method for target %s field %s method %s desc %s occur exception!",
              target.getClass().getName(), field, methodName, desc), e);
      return null;
    }
  }


  public static boolean checkEnhanceMethodExists(Class[] loadedClasses, Map<String, Object> methodMeta) {
    String clazzName = (String) methodMeta.get(CLASS_NAME);
    String methodName = (String) methodMeta.get(METHOD_NAME);
    String[] paramTypes = (String[]) methodMeta.get(PARAM_TYPES);
    // method是否存在
    // class是否存在
    boolean classExists = false, methodExists = false;

    for (Class target : loadedClasses) {
      if (!target.getName().equals(clazzName)) {
        continue;
      }
      if (target.isInterface()) {
        logger.log(Level.SEVERE, String.format("Can`t enhance interface %s for data collect enhance!", clazzName));
        break;
      }
      classExists = true;
      methodMeta.put(CLASS, target);

      Class[] classes = new Class[0];
      if (paramTypes != null && paramTypes.length > 0) {
        classes = forClasses(target, paramTypes);
        if (classes == null) {
          break;
        }
      }
      try {
        Method method = ReflectionUtils.findMethod(target, methodName, classes);
        if (methodExists = method != null) {
          if (Modifier.isAbstract(method.getModifiers())) {
            break;
          }
          methodMeta.put(HASHCODE, method.hashCode());
          methodMeta.put(METHOD, method);
          if (Modifier.isStatic(method.getModifiers())) {
            methodMeta.put(STATIC_FLAG, true);
          }
          METHOD_DESC_CHECKED.put(methodMeta.get(DESC), method);
        }
      } catch (Exception e) {
      }
      break;
    }
    if (!classExists) {
      logger.log(Level.SEVERE, String.format("Can`t find loaded class %s for data collect enhance!", clazzName));
    }
    if (!methodExists) {
      logger.log(Level.SEVERE, String.format("Can`t find data collect enhance target methodName %s(...) in class %s",
          methodName, clazzName));
    }
    return methodExists;
  }


  private static Class[] forClasses(Class type, String[] paramTypes) {
    Class[] classes = new Class[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      String paramType = paramTypes[i].trim();
      if (paramType.contains("<")) {
        paramType = paramType.substring(0, paramType.indexOf("<"));
      }
      if (!paramType.contains(".")) {
        Class simpleClass = mappingPrimitiveClass(paramType);
        if (simpleClass == null) {
          logger.log(Level.SEVERE, "Illegal param type" + paramType);
          return null;
        }
        classes[i] = simpleClass;
        continue;
      }
      try {
        classes[i] = type.getClassLoader().loadClass(paramType);
      } catch (Exception e) {
        logger.log(Level.SEVERE, String.format("Load class %s by classLoader %s failed", paramType,
            type.getClassLoader().getClass().getName()), e);
        return null;
      }
    }
    return classes;
  }

  private static Class mappingPrimitiveClass(String paramType) {
    switch (paramType) {
      case "int":
        return int.class;
      case "long":
        return long.class;
      case "float":
        return float.class;
      case "double":
        return double.class;
      case "byte":
        return byte.class;
      case "char":
        return char.class;
      case "boolean":
        return boolean.class;
      case "short":
        return short.class;
      default:
        return null;
    }
  }


}
