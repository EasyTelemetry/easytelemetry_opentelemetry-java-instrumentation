package io.easytelemetry.instrumentation.api.spy;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/9
 */
public class SpyAPI {

  // ############################################ 方法参数/返回值采集 ############################################

  public static final AbstractExtractSpy EXTRACT_METHOD_NOP_SPY = new NO_EXTRACT_SPY();
  private static volatile AbstractExtractSpy extractMethodSpyInstance = EXTRACT_METHOD_NOP_SPY;

  public static void setExtractMethodSpy(AbstractExtractSpy spyInstance) {
    SpyAPI.extractMethodSpyInstance = spyInstance;
  }

  public static abstract class AbstractExtractSpy {
    public abstract void extractParam(int index, Object param);

    public abstract void extractReturn(int index, Object ret);
  }

  public static class NO_EXTRACT_SPY extends AbstractExtractSpy {
    public void extractParam(int index, Object param) {
    }
    public void extractReturn(int index, Object ret) {
    }
  }

  public static void extractParam(int index, Object variableValue) {
    extractMethodSpyInstance.extractParam(index, variableValue);
  }

  public static void extractReturn(int index, Object variableValue) {
    extractMethodSpyInstance.extractReturn(index, variableValue);
  }

  // ############################################ 局部变量采集 ############################################

  public static final AbstractExtractVariableSpy EXTRACT_VARIABLE_NOP_SPY = new NO_EXTRACT_VARIABLE_SPY();
  private static volatile AbstractExtractVariableSpy extractVariableSpyInstance = EXTRACT_VARIABLE_NOP_SPY;

  public static void setExtractVariableSpy(AbstractExtractVariableSpy spyInstance) {
    SpyAPI.extractVariableSpyInstance = spyInstance;
  }

  public static abstract class AbstractExtractVariableSpy {
    public abstract void extractVariable(int index, Object variableValue);
  }

  public static void extractVariable(int index, Object variableValue) {
    extractVariableSpyInstance.extractVariable(index, variableValue);
  }

  public static class NO_EXTRACT_VARIABLE_SPY extends AbstractExtractVariableSpy {
    @Override
    public void extractVariable(int index, Object variableValue) {
    }
  }

}
