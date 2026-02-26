package io.opentelemetry.javaagent.instrumentation.undertow;

import io.easytelemetry.instrumentation.api.config.apply.extract.http.HttpRequestDataFetcher;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import java.util.Deque;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/1/1
 */
public class UndertowRequestDataExtractor implements HttpRequestDataFetcher<HttpServerExchange> {

  public static final UndertowRequestDataExtractor INSTANCE = new UndertowRequestDataExtractor();

  public static final AttachmentKey<Boolean> FETCHER_FLAG = AttachmentKey.create(Boolean.class);

  private UndertowRequestDataExtractor() {
  }

  @Override
  public String getRequestHeader(HttpServerExchange request, String headerName) {
    return request.getRequestHeaders().getFirst(headerName);
  }

  @Override
  public String getRequestParameter(HttpServerExchange request, String paramName) {
    Deque<String> strings = request.getPathParameters().get(paramName);
    if (strings != null && strings.size() > 0) {
      return strings.getFirst();
    }
    return null;
  }

  @Override
  public String getRequestUrl(HttpServerExchange request) {
    return request.getRequestURL();
  }

  @Override
  public void markFetchedFlag(HttpServerExchange request) {
    request.putAttachment(FETCHER_FLAG, Boolean.TRUE);
  }

  @Override
  public boolean hasFetchedBefore(HttpServerExchange request) {
    return request.getAttachment(FETCHER_FLAG) != null;
  }
}
