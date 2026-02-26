package io.easytelemetry.instrumentation.api.config.apply.extract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/3
 */
public class DataExtractingEntityComposite {

  public static final DataExtractingEntityComposite EMPTY = new DataExtractingEntityComposite(Collections.EMPTY_LIST);

  private boolean hasBodyConfig;
  private boolean hasContextConfig;
  private boolean hasJavaMethodConfig;
  private List<DataExtractingEntity> dataExtractingEntities;

  private DataExtractingEntity[] requestBodyDataExtractingEntities;
  private DataExtractingEntity[] responseBodyDataExtractingEntities;
  private DataExtractingEntity[] requestParamDataExtractingEntities;
  private DataExtractingEntity[] requestUriDataExtractingEntities;
  private DataExtractingEntity[] requestHeaderDataExtractingEntities;
  private DataExtractingEntity[] javaMethodExtractingEntities;
  private DataExtractingEntity[] javaMethodVariableExtractingEntities;

  // 索引过的

  public DataExtractingEntityComposite(List<DataExtractingEntity> dataExtractingEntities) {
    this.dataExtractingEntities = dataExtractingEntities;
    List<DataExtractingEntity> respBodyEntities = new ArrayList<>();
    List<DataExtractingEntity> paramEntities = new ArrayList<>();
    List<DataExtractingEntity> reqBodyEntities = new ArrayList<>();
    List<DataExtractingEntity> reqHeaderEntities = new ArrayList<>();
    List<DataExtractingEntity> reqUriEntities = new ArrayList<>();
    List<DataExtractingEntity> methodEntities = new ArrayList<>();
    List<DataExtractingEntity> variableEntities = new ArrayList<>();

    for (DataExtractingEntity entity : dataExtractingEntities) {
      switch (entity.getSourceType()) {
        case HTTP_REQUEST_BODY:
          reqBodyEntities.add(entity);
          break;
        case HTTP_RESPONSE_BODY:
          respBodyEntities.add(entity);
          break;
        case HTTP_REQUEST_PARAMETER:
          paramEntities.add(entity);
          break;
        case HTTP_REQUEST_HEADER:
          reqHeaderEntities.add(entity);
        case HTTP_REQUEST_URL:
          reqUriEntities.add(entity);
          break;
        case JAVA_METHOD_PARAMETERS:
        case JAVA_METHOD_RETURN_VALUE:
          methodEntities.add(entity);
          break;
        case JAVA_METHOD_LOCAL_VARIABLE:
          variableEntities.add(entity);
          break;
      }
    }
    this.hasContextConfig = !paramEntities.isEmpty()
        || !reqHeaderEntities.isEmpty()
        || !reqUriEntities.isEmpty();

    this.hasJavaMethodConfig = !methodEntities.isEmpty() || !variableEntities.isEmpty();

    this.hasBodyConfig = !reqBodyEntities.isEmpty() || !respBodyEntities.isEmpty();

    this.requestBodyDataExtractingEntities = reqBodyEntities.toArray(new DataExtractingEntity[0]);
    this.responseBodyDataExtractingEntities = respBodyEntities.toArray(new DataExtractingEntity[0]);
    this.requestParamDataExtractingEntities = paramEntities.toArray(new DataExtractingEntity[0]);
    this.requestUriDataExtractingEntities = reqUriEntities.toArray(new DataExtractingEntity[0]);
    this.requestHeaderDataExtractingEntities = reqHeaderEntities.toArray(new DataExtractingEntity[0]);
    this.javaMethodExtractingEntities = methodEntities.toArray(new DataExtractingEntity[0]);
    this.javaMethodVariableExtractingEntities = variableEntities.toArray(new DataExtractingEntity[0]);
  }

  public List<DataExtractingEntity> getDataExtractingEntities() {
    return dataExtractingEntities;
  }

  public DataExtractingEntity[] getRequestBodyDataExtractingEntities() {
    return requestBodyDataExtractingEntities;
  }

  public DataExtractingEntity[] getResponseBodyDataExtractingEntities() {
    return responseBodyDataExtractingEntities;
  }

  public DataExtractingEntity[] getRequestParamDataExtractingEntities() {
    return requestParamDataExtractingEntities;
  }

  public DataExtractingEntity[] getRequestUriDataExtractingEntities() {
    return requestUriDataExtractingEntities;
  }

  public DataExtractingEntity[] getRequestHeaderDataExtractingEntities() {
    return requestHeaderDataExtractingEntities;
  }

  public DataExtractingEntity[] getJavaMethodExtractingEntities() {
    return javaMethodExtractingEntities;
  }

  public boolean isHasContextConfig() {
    return hasContextConfig;
  }

  public boolean isHasBodyConfig() {
    return hasBodyConfig;
  }

  public boolean isHasJavaMethodConfig() {
    return hasJavaMethodConfig;
  }

  public DataExtractingEntity[] getJavaMethodVariableExtractingEntities() {
    return javaMethodVariableExtractingEntities;
  }
}
