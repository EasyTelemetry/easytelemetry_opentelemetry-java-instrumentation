package io.easytelemetry.javaagent.tooling.config.applier;

import io.easytelemetry.instrumentation.api.ETelConfig;
import io.easytelemetry.instrumentation.api.config.ETelConfigApplier;
import io.easytelemetry.instrumentation.api.config.apply.extract.DataExtractingEntity;
import io.easytelemetry.instrumentation.api.config.apply.extract.LocalVariableExtractor;
import io.easytelemetry.javaagent.tooling.extract.variable.VariableExtractLineIntrospection;
import io.easytelemetry.javaagent.tooling.extract.variable.VariableExtractLineTransformer;
import io.easytelemetry.javaagent.tooling.extract.variable.VariableExtractTransformer;
import io.easytelemetry.javaagent.tooling.util.InstrumentationUtils;
import io.easytelemetry.javaagent.tooling.util.MethodUtil;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/3
 */
public class VariableExtractApplier implements ETelConfigApplier {

  private static final Logger logger = Logger.getLogger(VariableExtractApplier.class.getName());

  public static final VariableExtractApplier INSTANCE = new VariableExtractApplier();

  // javaMethodLocalVariableExtracting
  private static final Map<Method, ClassFileTransformer> LOCAL_VARIABLE_EXTRACTING_TRANSFORMER_MAPPINGS = new ConcurrentHashMap<>();

  private VariableExtractApplier() {}

  @Override
  public void apply(Map<String, Object> config) throws Throwable {
    DataExtractingEntity[] entities = ETelConfig.getDataExtractingConfigs()
        .getJavaMethodVariableExtractingEntities();
    DataExtractingEntity[] indexEntities = ETelConfig.getIndexedVariableExtractingEntities();

    Set<Integer> newEntityIndexes = new HashSet<>();
    Set<Integer> existsEntityIndexes = new HashSet<>();
    Set<Integer> removedEntityIndexes = new HashSet<>();
    Set<Integer> pendingRemovedEntityIndexes = new HashSet<>();

    for (DataExtractingEntity entity : entities) {
      Map<String, Object> methodMeta = entity.getJavaMethodMeta();
      if (methodMeta == null) {
        logger.log(Level.SEVERE, "Missing method desc");
        continue;
      }

      String desc = (String) methodMeta.get(DESC);
      if (!MethodUtil.hasCheckedMethod(desc)) {
        if (!MethodUtil.checkEnhanceMethodExists(InstrumentationUtils.getLoadedClasses(), methodMeta)) {
          return;
        }
      }
      Method method = MethodUtil.getMethod(desc);
      methodMeta.put(METHOD, method);
      methodMeta.put(HASHCODE, method.hashCode());

      int entityIndex = findEntityIndex(entity);
      if (entityIndex == -1) {
        int indexed = indexVariableExtractEntity(entity);
        entity.setEntityIndex(indexed);
        newEntityIndexes.add(indexed);
      } else {
        existsEntityIndexes.add(entityIndex);
        entity.setEntityIndex(entityIndex);
      }
    }

    for (int i = 0; i < indexEntities.length; i++) {
      DataExtractingEntity entity = indexEntities[i];
      if (entity == null) {
        continue;
      }
      Integer entityIndex = entity.getVariableConfigIndex();
      if (newEntityIndexes.contains(entityIndex) || existsEntityIndexes.contains(entityIndex)) {
        continue;
      }
      removedEntityIndexes.add(entityIndex);
    }

    Iterator<Integer> iterator = removedEntityIndexes.iterator();
    while (iterator.hasNext()) {
      Integer index = iterator.next();
      DataExtractingEntity entity = indexEntities[index];
      if (entity == null) {
        continue;
      }
      String desc = (String) entity.getJavaMethodMeta().get(DESC);
      boolean stillExists = Arrays.stream(entities)
          .filter(et -> et.getJavaMethodMeta().get(DESC).equals(desc)).findAny().isPresent();
      if (stillExists) {
        iterator.remove();
        pendingRemovedEntityIndexes.add(index);
      }
    }

    // ******************************** 删除 ********************************

    Set<Method> resetMethods = removedEntityIndexes.stream().map(new Function<Integer, Method>() {
      @Override
      public Method apply(Integer integer) {
        return (Method) indexEntities[integer].getJavaMethodMeta().get(METHOD);
      }
    }).collect(Collectors.toSet());

    for (Method resetMethod : resetMethods) {
      ClassFileTransformer classFileTransformer = LOCAL_VARIABLE_EXTRACTING_TRANSFORMER_MAPPINGS.remove(resetMethod);
      if (classFileTransformer != null) {
        InstrumentationHolder.getInstrumentation().removeTransformer(classFileTransformer);
      }
    }
    // 还原Method
    if (!resetMethods.isEmpty()) {
      Set<Class> resetClasses = resetMethods.stream()
          .map((Function<Method, Class>) method -> method.getDeclaringClass()).collect(Collectors.toSet());
      Class[] classes = new ArrayList<>(resetClasses).toArray(new Class[0]);
      try {
        InstrumentationHolder.getInstrumentation().retransformClasses(classes);
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Failed to reset transformation", t);
      }
    }

    for (Integer index : removedEntityIndexes) {
      indexEntities[index] = null;
    }

    // ******************************** 新增/修改 ********************************
    Set<Method> pendingMethods = new HashSet<>();
    for (Integer entityIndex : newEntityIndexes) {
      DataExtractingEntity entity = indexEntities[entityIndex];
      Method method = (Method) entity.getJavaMethodMeta().get(METHOD);
      pendingMethods.add(method);
    }
    for (Integer entityIndex : pendingRemovedEntityIndexes) {
      DataExtractingEntity entity = indexEntities[entityIndex];
      Method method = (Method) entity.getJavaMethodMeta().get(METHOD);
      indexEntities[entityIndex] = null;
      pendingMethods.add(method);
    }

    Instrumentation inst = InstrumentationHolder.getInstrumentation();

    for (Method method : pendingMethods) {
      Map<String, List<LocalVariableExtractor>> variableEntities = new HashMap<>();
      for (DataExtractingEntity entity : indexEntities) {
        if (entity == null) {
          continue;
        }
        if (entity.getJavaMethodMeta().get(METHOD).equals(method)) {
          String variable = entity.getVariable();
          List<LocalVariableExtractor> methodEntities = variableEntities.computeIfAbsent(variable,
              k -> new ArrayList<>());
          methodEntities.add(entity);
        }
      }
      if (variableEntities.isEmpty()) {
        logger.log(Level.WARNING, "Server Error");
        continue;
      }

      String desc = MethodUtil.getMethodDescriptor(method);

      VariableExtractLineIntrospection lineIntrospection = new VariableExtractLineIntrospection(variableEntities);
      VariableExtractLineTransformer lineTransformer = new VariableExtractLineTransformer(method,
          MethodUtil.getMethodDescriptor(method), lineIntrospection);

      try {
        inst.addTransformer(lineTransformer, true);
        inst.retransformClasses(method.getDeclaringClass());
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Can`t retransform local variable extracting method: " + desc, e);
      } finally {
        inst.removeTransformer(lineTransformer);
      }

      String errorMsg = lineIntrospection.getErrorMsg();
      if (errorMsg != null) {
        logger.log(Level.SEVERE,
            "Can`t retransform local variable extracting method: " + desc + ", errorMsg:" + errorMsg);
        continue;
      }

      lineIntrospection.refresh();

      VariableExtractTransformer transformer = new VariableExtractTransformer(method, lineIntrospection);

      try {
        ClassFileTransformer oldTransformer = LOCAL_VARIABLE_EXTRACTING_TRANSFORMER_MAPPINGS.remove(method);
        if (oldTransformer != null) {
          inst.removeTransformer(oldTransformer);
        }
        inst.addTransformer(transformer, true);
        inst.retransformClasses(method.getDeclaringClass());
        logger.log(Level.INFO, "Success retransform local variable extracting method: " + desc);
        LOCAL_VARIABLE_EXTRACTING_TRANSFORMER_MAPPINGS.put(method, transformer);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Can`t retransform trace method: " + desc, e);
        inst.removeTransformer(transformer);
        return;
      }
    }

  }

  private int indexVariableExtractEntity(DataExtractingEntity entity) {
    DataExtractingEntity[] entities = ETelConfig.getIndexedVariableExtractingEntities();

    for (int i = 0; i < entities.length; i++) {
      if (entities[i] == null) {
        entities[i] = entity;
        return i;
      }
    }

    DataExtractingEntity[] growEntities = new DataExtractingEntity[entities.length << 1];
    System.arraycopy(entities, 0, growEntities, 0, entities.length);
    ETelConfig.setIndexedVariableExtractingEntities(growEntities);

    growEntities[entities.length] = entity;
    return entities.length;
  }

  private int findEntityIndex(DataExtractingEntity entity) {
    DataExtractingEntity[] entities = ETelConfig.getIndexedVariableExtractingEntities();
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


}
