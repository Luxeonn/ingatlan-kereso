package ingatlanok.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Shared Jackson 3 JSON parsing helpers for portal payloads. */
public final class JsonSupport {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonSupport() {}

  public static JsonNode parse(String json) throws JacksonException {
    return MAPPER.readTree(json);
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
