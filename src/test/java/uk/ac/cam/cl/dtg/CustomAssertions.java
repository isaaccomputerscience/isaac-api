package uk.ac.cam.cl.dtg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CustomAssertions {
  private static final ObjectMapper jsonMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  public static void assertDeepEquals(Object expected, Object actual) {
    try {
      assertEquals(jsonMapper.writeValueAsString(expected), jsonMapper.writeValueAsString(actual));
    } catch (JsonProcessingException e) {
      fail("Error converting object to json string for comparison", e);
    }
  }
}
