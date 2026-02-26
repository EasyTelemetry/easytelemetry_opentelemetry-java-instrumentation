package io.opentelemetry.javaagent.instrumentation.tomcat.v10_0;

import io.easytelemetry.instrumentation.api.config.apply.extract.http.HttpRequestDataFetcher;
import org.apache.coyote.Request;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/1
 */
public class Tomcat10RequestDataExtractor implements HttpRequestDataFetcher<Request> {

  public static final Tomcat10RequestDataExtractor INSTANCE = new Tomcat10RequestDataExtractor();

  private Tomcat10RequestDataExtractor() {
  }
  @Override
  public String getRequestHeader(Request request, String headerName) {
    return request.getHeader(headerName);
  }

  @Override
  public String getRequestParameter(Request request, String paramName) {
    return request.getParameters().getParameter(paramName);
  }

  @Override
  public String getRequestUrl(Request request) {
    return request.requestURI().toString();
  }

  @Override
  public void markFetchedFlag(Request request) {
    request.setAttribute(FETCHED_FLAG, Boolean.TRUE);
  }

  @Override
  public boolean hasFetchedBefore(Request request) {
    return request.getAttribute(FETCHED_FLAG) != null;
  }
}
