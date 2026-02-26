package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;

import io.easytelemetry.instrumentation.api.config.apply.extract.http.HttpRequestDataFetcher;

import javax.servlet.http.HttpServletRequest;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/1
 */
public class Servlet3RequestDataExtractor implements HttpRequestDataFetcher<HttpServletRequest> {

  public static final Servlet3RequestDataExtractor INSTANCE = new Servlet3RequestDataExtractor();

  private Servlet3RequestDataExtractor() {
  }

  @Override
  public String getRequestHeader(HttpServletRequest request, String headerName) {
    return request.getHeader(headerName);
  }

  @Override
  public String getRequestParameter(HttpServletRequest request, String paramName) {
    return request.getParameter(paramName);
  }

  @Override
  public String getRequestUrl(HttpServletRequest request) {
    return request.getRequestURI();
  }

  @Override
  public void markFetchedFlag(HttpServletRequest request) {
    request.setAttribute(FETCHED_FLAG, Boolean.TRUE);
  }

  @Override
  public boolean hasFetchedBefore(HttpServletRequest request) {
    return request.getAttribute(FETCHED_FLAG) != null;
  }
}
