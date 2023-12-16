package com.howhow.functions.utils;

import com.howhow.functions.exception.HttpClientErrorException;
import com.howhow.functions.exception.HttpServerErrorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class HttpClientUtils {
  private static final String REQUEST_LOG = "request url:%s, method:%s, header:%s\n body:%s";
  private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 30 * 1000;
  private static final Logger LOGGER = Logger.getLogger(HttpClientUtils.class.getName());
  private static final HttpClientConnectionManager connPoolManager = configConnPool();
  private static final CloseableHttpClient httpClient =
      HttpClients.custom()
          .setConnectionManager(connPoolManager)
          .setKeepAliveStrategy(configKeepAliveStrategy())
          .evictExpiredConnections()
          .evictIdleConnections(10, TimeUnit.SECONDS)
          .build();

  public static String postUrlFormRequest(
      String targetUrl, Map<String, String> headers, List<NameValuePair> formData)
      throws IOException {

    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formData, Consts.UTF_8);
    HttpPost httpPost = new HttpPost(targetUrl);

    configHeaders(httpPost, headers);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
    httpPost.setEntity(entity);
    return sendRequest(httpPost);
  }

  public static String postJsonRequest(String targetUrl, String postBody) throws IOException {

    HttpPost request = new HttpPost(targetUrl);
    request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    request.setEntity(new StringEntity(postBody, Consts.UTF_8));
    return sendRequest(request);
  }

  public static <T> T postRequest(String targetUrl, Object postBody, Class<T> responseType)
      throws IOException {
    return JsonUtils.toObject(postRequest(targetUrl, postBody), responseType);
  }

  public static String postRequest(String targetUrl, Object postBody) throws IOException {
    HttpPost request = new HttpPost(targetUrl);

    request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    request.setEntity(new StringEntity(JsonUtils.toJsonString(postBody), Consts.UTF_8));
    return sendRequest(request);
  }

  public static String getRequest(String targetUrl, Map<String, String> headers)
      throws IOException {
    HttpGet request = new HttpGet(targetUrl);
    configHeaders(request, headers);
    return sendRequest(request);
  }

  private static String sendRequest(HttpUriRequest request) throws IOException {

    try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
      String responseBody = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
      int responseStatus = httpResponse.getStatusLine().getStatusCode();

      if (responseStatus != 200) {
        int statusSerial = responseStatus / 100;
        logRequest(request);

        switch (statusSerial) {
          case 4:
            throw new HttpClientErrorException(responseStatus, responseBody);
          case 5:
            throw new HttpServerErrorException(responseStatus, responseBody);
        }
      }
      EntityUtils.consume(httpResponse.getEntity());
      return responseBody;
    } catch (IOException e) {
      logRequest(request);
      throw e;
    }
  }

  private static void logRequest(HttpUriRequest request) throws IOException {

    String requestBody = "";
    if (request instanceof HttpPost) {
      requestBody = EntityUtils.toString(((HttpPost) request).getEntity(), StandardCharsets.UTF_8);
    }
    LOGGER.warning(
        String.format(
            REQUEST_LOG,
            request.getURI().toString(),
            request.getMethod(),
            getHeaderData(request),
            requestBody));
  }

  private static String getHeaderData(HttpUriRequest request) {
    Map<String, String> headerMap = new HashMap<>();

    String contentType = request.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
    headerMap.put(HttpHeaders.CONTENT_TYPE, contentType);

    Header authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null) {
      headerMap.put(HttpHeaders.AUTHORIZATION, authorization.getValue());
    }
    return headerMap.toString();
  }

  private static void configHeaders(HttpUriRequest request, Map<String, String> headerData) {
    for (Map.Entry<String, String> data : headerData.entrySet()) {
      request.setHeader(data.getKey(), data.getValue());
    }
  }

  private static HttpClientConnectionManager configConnPool() {
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    connManager.setMaxTotal(50);
    connManager.setDefaultMaxPerRoute(10);

    return connManager;
  }

  private static RequestConfig requestConfig() {
    RequestConfig requestConfig =
        RequestConfig.custom().setConnectTimeout(30 * 1000).setSocketTimeout(60 * 1000).build();
    return requestConfig;
  }

  private static DefaultProxyRoutePlanner proxyRouter() {

    String proxyHost = System.getenv("proxyHost");
    String proxyPort = System.getenv("proxyPort");
    if (!StringUtils.isEmpty(proxyHost) && !StringUtils.isEmpty(proxyPort)) {
      Integer port = Integer.parseInt(proxyPort);
      return new DefaultProxyRoutePlanner(new HttpHost(proxyHost, port));
    }
    return null;
  }

  private static ConnectionKeepAliveStrategy configKeepAliveStrategy() {
    return new DefaultConnectionKeepAliveStrategy() {
      @Override
      public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long keepAliveDuration = super.getKeepAliveDuration(response, context);
        if (keepAliveDuration < 0) {
          return DEFAULT_KEEP_ALIVE_TIME_MILLIS;
        }
        return keepAliveDuration;
      }
    };
  }


  public static String get(String url) {
    HttpGet httpGet = new HttpGet(url);
    httpGet.setConfig(requestConfig());
    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.warning("get request error: " + e.getMessage());
      return "";
    }
  }
}
