package uk.ac.cam.cl.dtg.util;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

import java.util.Map;

public final class LogUtils {
  private LogUtils() {
  }

  /**
   * Sanitise a value to be logged.
   * The called function will escape any characters outside the unicode range of 32-127 (Latin and ASCII alphabet,
   * symbols and digits).
   *
   * @param value the String to be sanitised
   * @return the sanitised String
   */
  public static String sanitiseLogValue(final String value) {
    return escapeJava(value);
  }

  /**
   * Sanitise a value to be logged. This value should be provided by internal sources such as admin accounts or
   * platform infrastructure functions and thus clean, but it may be necessary to guard against malicious forged
   * requests.
   *
   * @param value the String to be sanitised
   * @return the sanitised String
   */
  public static String sanitiseInternalLogValue(final String value) {
    return sanitiseLogValue(value);
  }

  /**
   * Sanitise a value to be logged. This value is expected to be selected or provided by general users, though the
   * front-end may apply some validation to the input under normal use cases.
   *
   * @param value the String to be sanitised
   * @return the sanitised String
   */
  public static String sanitiseUserLogValue(final String value) {
    return sanitiseLogValue(value);
  }

  public static String sanitiseUserLogValue(final Map<String, Object> value) {
    if (value != null) {
      return sanitiseUserLogValue(value.toString());
    }
    return null;
  }
}
