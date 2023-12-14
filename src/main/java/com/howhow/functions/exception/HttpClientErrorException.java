package com.howhow.functions.exception;

import org.apache.http.client.HttpResponseException;

public class HttpClientErrorException extends HttpResponseException {
  public HttpClientErrorException(int statusCode) {
    super(statusCode, null);
  }

  public HttpClientErrorException(int statusCode, String reasonPhrase) {
    super(statusCode, reasonPhrase);
  }
}
