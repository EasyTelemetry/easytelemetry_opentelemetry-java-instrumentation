package io.easytelemetry.javaagent.tooling.thread;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ETelThreadFactory implements ThreadFactory {

  private static final Logger logger = Logger.getLogger(ETelThreadFactory.class.getName());

  public static final ThreadGroup THREAD_GROUP = new ThreadGroup("easy-telemetry");

  public enum AgentThread {
    HEARTBEAT("easy-telemetry-heartbeat"),
    CONFIG_FETCHER("easy-telemetry-config-fetcher");

    public final String threadName;

    AgentThread(final String threadName) {
      this.threadName = threadName;
    }

    public static Set<String> threadNames = new HashSet<>();

    static {
      for (AgentThread value : AgentThread.values()) {
        threadNames.add(value.threadName);
      }
    }

    public static Set<String> getThreadNames() {
      return threadNames;
    }
  }

  private final AgentThread agentThread;

  /**
   * Constructs a new agent {@code ThreadFactory}.
   *
   * @param agentThread the agent thread created by this factory.
   */
  public ETelThreadFactory(AgentThread agentThread) {
    this.agentThread = agentThread;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return newAgentThread(agentThread, runnable);
  }

  /**
   * Constructs a new agent {@code Thread} as a daemon with a null ContextClassLoader.
   *
   * @param agentThread the agent thread to create.
   * @param runnable work to run on the new thread.
   */
  public static Thread newAgentThread(AgentThread agentThread, Runnable runnable) {
    Thread thread = new Thread(THREAD_GROUP, runnable, agentThread.threadName);
    thread.setDaemon(true);
    thread.setContextClassLoader(null);
    thread.setUncaughtExceptionHandler(
        new Thread.UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread thread, Throwable e) {
            logger.log(Level.SEVERE, "Uncaught exception in " + agentThread.threadName, e);
          }
        });
    return thread;
  }
}
