package io.opentelemetry.javaagent.instrumentation.servlet.v5_0;

import io.easytelemetry.instrumentation.api.config.apply.extract.http.HttpRequestDataFetcher;
import jakarta.servlet.http.HttpServletRequest;


/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/1
 */
public class Servlet5RequestDataExtractor implements HttpRequestDataFetcher<HttpServletRequest> {

  public static final Servlet5RequestDataExtractor INSTANCE = new Servlet5RequestDataExtractor();

  private Servlet5RequestDataExtractor() {
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
