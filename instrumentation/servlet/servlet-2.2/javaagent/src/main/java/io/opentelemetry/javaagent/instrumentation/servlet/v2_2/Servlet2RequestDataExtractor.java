package io.opentelemetry.javaagent.instrumentation.servlet.v2_2;

import io.easytelemetry.instrumentation.api.config.apply.extract.http.HttpRequestDataFetcher;
import javax.servlet.http.HttpServletRequest;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/1
 */
public class Servlet2RequestDataExtractor implements HttpRequestDataFetcher<HttpServletRequest> {

  public static final Servlet2RequestDataExtractor INSTANCE = new Servlet2RequestDataExtractor();

  private Servlet2RequestDataExtractor() {
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
