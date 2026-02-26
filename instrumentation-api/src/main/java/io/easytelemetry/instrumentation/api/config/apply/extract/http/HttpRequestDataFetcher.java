package io.easytelemetry.instrumentation.api.config.apply.extract.http;

public interface HttpRequestDataFetcher<T> {

  String FETCHED_FLAG = "etel.data_fetcher";

  String getRequestHeader(T request, String headerName);

  String getRequestParameter(T request, String paramName);

  String getRequestUrl(T request);

  void markFetchedFlag(T request);

  boolean hasFetchedBefore(T request);

}
