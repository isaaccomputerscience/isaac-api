package uk.ac.cam.cl.dtg.util.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MapperTestUtils {
  private static final ObjectMapper jsonMapper = new ObjectMapper();

  public static void assertDeepEquals(Object expected, Object actual) throws JsonProcessingException {
    assertEquals(jsonMapper.writeValueAsString(expected), jsonMapper.writeValueAsString(actual));
  }
}
