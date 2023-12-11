package com.howhow.functions.utils;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.FixedDelayOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.storage.queue.QueueAsyncClient;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import java.net.InetSocketAddress;
import java.time.Duration;

public class QueueUtils {
  private static final Integer retryMaxTimes = 3;
  private static final Integer retryDelayTimeout = 3;

  private static final HttpClient httpClient = new NettyAsyncHttpClientBuilder().build();

  public static QueueClient createQueueClient(String queueName, String connString) {
    return new QueueClientBuilder()
        .retryOptions(configRetryOptions())
        .httpClient(httpClient)
        .queueName(queueName)
        .connectionString(connString)
        .buildClient();
  }

  public static QueueAsyncClient createAsyncQueueClient(String queueName, String connString) {
    return new QueueClientBuilder()
        .retryOptions(configRetryOptions())
        .httpClient(httpClient)
        .queueName(queueName)
        .connectionString(connString)
        .buildAsyncClient();
  }

  private static RetryOptions configRetryOptions() {
    FixedDelayOptions fixedDelayOptions =
        new FixedDelayOptions(retryMaxTimes, Duration.ofMillis(retryDelayTimeout));
    return new RetryOptions(fixedDelayOptions);
  }

  private static ProxyOptions configProxy() {
    String proxyHost = System.getenv("proxyHost");
    String proxyPort = System.getenv("proxyPort");
    return new ProxyOptions(
        ProxyOptions.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
  }

  public static String getDefaultConnString() {
    return System.getenv("AzureWebJobsStorage");
  }
}
