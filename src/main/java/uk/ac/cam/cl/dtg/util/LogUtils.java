package uk.ac.cam.cl.dtg.util;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

public final class LogUtils {
    private LogUtils() { }

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
     * Sanitise a value to be logged. This value should be derived from a content object.
     * This should normally be provided by the platform, but it is theoretically possible to provide the endpoint a
     * request with a forged but validly formatted object.
     *
     * @param value the String to be sanitised
     * @return the sanitised String
     */
    public static String sanitiseContentLogValue(final String value) {
        return sanitiseLogValue(value);
    }

    /**
     * Sanitise a value to be logged. This value should be provided by internal sources such as admin accounts or
     * platform infrastructure functions.
     *
     * @param value the String to be sanitised
     * @return the sanitised String
     */
    public static String sanitiseInternalLogValue(final String value) {
        return sanitiseLogValue(value);
    }
}
