package uk.ac.cam.cl.dtg.util;

public class LogUtils {
    public static String sanitiseLogValue(String value) {
        return value.replaceAll("(\\s|\\n)+", "");
    }
}
