package com.howhow.functions.exception;

import org.apache.http.client.HttpResponseException;

public class HttpServerErrorException extends HttpResponseException {
  public HttpServerErrorException(int statusCode) {
    super(statusCode, null);
  }

  public HttpServerErrorException(int statusCode, String reasonPhrase) {
    super(statusCode, reasonPhrase);
  }
}
