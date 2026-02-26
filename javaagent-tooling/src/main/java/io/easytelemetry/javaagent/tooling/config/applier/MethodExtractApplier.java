package io.easytelemetry.javaagent.tooling.config.applier;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.ETelConfigApplier;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingSource;
import io.easytelemetry.javaagent.tooling.extract.method.ExtractingMethodTransformer;
import io.easytelemetry.javaagent.tooling.util.InstrumentationUtils;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/3
 */
public class MethodExtractApplier implements ETelConfigApplier {

  private static final Logger logger = Logger.getLogger(MethodExtractApplier.class.getName());

  public static final MethodExtractApplier INSTANCE = new MethodExtractApplier();

  private static final Map<Class, ClassFileTransformer> CLASS_TRANSFORMERS = new ConcurrentHashMap<>();

  private MethodExtractApplier() {}

  @Override
  public void apply(Map<String, Object> config) throws Throwable {
    DataExtractingEntity[] entities = ETelConfig.getDataExtractingConfigs().getJavaMethodExtractingEntities();

    Set<Integer> newIndexes = new HashSet<>();
    Set<Integer> entityIndexes = new HashSet<>();

    for (DataExtractingEntity entity : entities) {
      Map<String, Object> methodMeta = entity.getJavaMethodMeta();
      if (methodMeta == null) {
        logger.log(Level.SEVERE, "Missing method desc");
        continue;
      }

      String[] paramTypes = (String[]) methodMeta.get(PARAM_TYPES);
      if (paramTypes == null) {
        logger.log(Level.SEVERE, "Can`t collect null params method");
        continue;
      }

      if (entity.getSourceType() == DataExtractingSource.JAVA_METHOD_PARAMETERS) {
        int paramIndex = entity.getExpression().charAt(3) - 48;
        if (paramIndex < 0 || paramIndex > paramTypes.length - 1) {
          logger.log(Level.SEVERE, "Illegal '$.[x]' index 'x' ! ");
          continue;
        }
      }

      String desc = (String) methodMeta.get(DESC);
      if (!MethodUtil.hasCheckedMethod(desc)) {
        if (!MethodUtil.checkEnhanceMethodExists(InstrumentationUtils.getLoadedClasses(), methodMeta)) {
          continue;
        }
      }
      Method method = MethodUtil.getMethod(desc);
      methodMeta.put(METHOD, method);
      methodMeta.put(HASHCODE, method.hashCode());

      int index = findEntityIndex(entity);
      if (index != -1) {
        entityIndexes.add(index);
        continue;
      }

      int newIndex = indexVariableExtractEntity(entity);
      newIndexes.add(newIndex);

    }

    Set<Class> pendingResetClasses = new HashSet<>();
    Set<Class> pendingRetransformClasses = new HashSet<>();
    DataExtractingEntity[] entityConfigs = ETelConfig.getIndexedMethodExtractingEntities();
    for (int i = 0; i < entityConfigs.length; i++) {
      DataExtractingEntity entity = entityConfigs[i];
      if (entity == null) {
        continue;
      }
      if (entityIndexes.contains(i)) {
        continue;
      }
      Method method = (Method) entity.getJavaMethodMeta().get(METHOD);
      if (newIndexes.contains(i)) {
        pendingRetransformClasses.add(method.getDeclaringClass());
        continue;
      }
      pendingResetClasses.add(method.getDeclaringClass());
      entityConfigs[i] = null;
    }

    for (Class clazz : pendingResetClasses) {
      boolean hasConfig = false;
      for (DataExtractingEntity entity : entityConfigs) {
        Method method = (Method) entity.getJavaMethodMeta().get(METHOD);
        if (method.getDeclaringClass() == clazz) {
          hasConfig = true;
          break;
        }
      }
      // reset
      if (!hasConfig) {
        resetClass(clazz);
      } else { // retransform
        pendingRetransformClasses.add(clazz);
      }
    }

    for (Class clazz : pendingRetransformClasses) {
      retransformClass(clazz);
    }

  }

  private int findEntityIndex(DataExtractingEntity entity) {
    DataExtractingEntity[] entities = ETelConfig.getIndexedMethodExtractingEntities();
    for (DataExtractingEntity e : entities) {
      if (e == null) {
        continue;
      }
      if (e.hashCode() == entity.hashCode()) {
        return e.getVariableConfigIndex();
      }
    }
    return -1;
  }

  private int indexVariableExtractEntity(DataExtractingEntity entity) {
    DataExtractingEntity[] entities = ETelConfig.getIndexedMethodExtractingEntities();

    for (int i = 0; i < entities.length; i++) {
      if (entities[i] == null) {
        entities[i] = entity;
        entity.setEntityIndex(i);
        return i;
      }
    }

    DataExtractingEntity[] growEntities = new DataExtractingEntity[entities.length << 1];
    System.arraycopy(entities, 0, growEntities, 0, entities.length);
    ETelConfig.setIndexedVariableExtractingEntities(growEntities);

    for (int i = 0; i < growEntities.length; i++) {
      if (growEntities[i] == null) {
        growEntities[i] = entity;
        return i;
      }
    }
    return -1;
  }


  private void resetClass(Class clazz) {
    try {
      Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
      ClassFileTransformer transformer = CLASS_TRANSFORMERS.remove(clazz);
      instrumentation.removeTransformer(transformer);
      instrumentation.retransformClasses(clazz);
      logger.log(Level.INFO, "Reset transformation for:" + clazz.getName());
    } catch (Throwable throwable) {
      logger.log(Level.SEVERE, "Reset transformation failed for:" + clazz.getName(), throwable);
    }
  }

  private void retransformClass(Class clazz) {
    try {
      Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
      ClassFileTransformer oldTransformer = CLASS_TRANSFORMERS.get(clazz);
      if (oldTransformer != null) {
        instrumentation.removeTransformer(oldTransformer);
      }
      Map<Method, List<DataExtractingEntity>> methodExtractingEntities = findMethodExtractingEntities(clazz);
      ExtractingMethodTransformer transformer = new ExtractingMethodTransformer(methodExtractingEntities);
      instrumentation.addTransformer(transformer, true);
      instrumentation.retransformClasses(clazz);
      CLASS_TRANSFORMERS.put(clazz, transformer);
      logger.log(Level.INFO, "Retransform class for:" + clazz.getName());
    } catch (Throwable throwable) {
      logger.log(Level.SEVERE, "Retransform class failed for:" + clazz.getName(), throwable);
    }
  }

  private Map<Method, List<DataExtractingEntity>> findMethodExtractingEntities(Class clazz) {
    Map<Method, List<DataExtractingEntity>> methodExtractingEntities = new HashMap<>();
    for (DataExtractingEntity entity : ETelConfig.getIndexedMethodExtractingEntities()) {
      if (entity == null) {
        continue;
      }
      Method method = (Method) entity.getJavaMethodMeta().get(METHOD);
      if (method.getDeclaringClass() == clazz) {
        methodExtractingEntities.computeIfAbsent(method, k -> new ArrayList<>()).add(entity);
      }
    }
    for (List<DataExtractingEntity> value : methodExtractingEntities.values()) {
      Collections.sort(value, Comparator.comparingInt(DataExtractingEntity::getVariableConfigIndex));
    }
    return methodExtractingEntities;
  }
}
