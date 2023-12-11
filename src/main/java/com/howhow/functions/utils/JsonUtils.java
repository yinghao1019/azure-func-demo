package com.howhow.functions.utils;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtils {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
              .registerModule(new JavaTimeModule())
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private JsonUtils() {}

  public static String toJsonString(Object o) {
    try {
      return MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public static Map<String, Object> toMap(String jsonString) {
    try {
      return MAPPER.readValue(jsonString, Map.class);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public static <T> T toObject(String jsonString, Class<T> targetClazz) {
    try {
      return MAPPER.readValue(jsonString, targetClazz);
    } catch (IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  public static <T> List<T> toList(String jsonString, Class<T> object) {
    TypeReference<List<T>> typeReference = new TypeReference<List<T>>() {};
    try {
      return MAPPER.readValue(jsonString, typeReference);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
