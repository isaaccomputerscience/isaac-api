package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static uk.ac.cam.cl.dtg.segue.api.Constants.GOOGLE_RECAPTCHA_SECRET;

public class RECAPTCHAManager {
    private final PropertiesLoader properties;
    private static final Logger log = LoggerFactory.getLogger(RECAPTCHAManager.class);

    @Inject
    public RECAPTCHAManager(final PropertiesLoader properties) {
        Validate.notNull(properties.getProperty(GOOGLE_RECAPTCHA_SECRET));
        this.properties = properties;
    }

    public String isCaptchaValid(final String response) {
        if (response == null || response.isEmpty()) {
            return "Missing reCAPTCHA response token.";
        }

        if (verifyRecaptcha(response)) {
            return "reCAPTCHA verification successful.";
        } else {
            return "reCAPTCHA verification failed.";
        }
    }

    protected JSONObject performHttpRequest(final String url, final String params) throws Exception {
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        http.setDoOutput(true);
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8");

        try (OutputStream out = http.getOutputStream()) {
            out.write(params.getBytes("UTF-8"));
            out.flush();
        }

        StringBuilder sb = new StringBuilder();
        try (InputStream res = http.getInputStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(res, "UTF-8"))) {

            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        } // End of try-with-resources block

        return new JSONObject(sb.toString());
    }



    public synchronized boolean verifyRecaptcha(final String response) {
        try {
            if (!response.isEmpty()) {
                String secretKey = properties.getProperty(GOOGLE_RECAPTCHA_SECRET);
                String url = "https://www.google.com/recaptcha/api/siteverify";
                String params = "secret=" + secretKey + "&response=" + response;

                JSONObject json = performHttpRequest(url, params);
                return json.getBoolean("success");
            }
        } catch (Exception e) {
            log.error("Error during reCAPTCHA validation.", e);
        }
        return false;
    }
}
