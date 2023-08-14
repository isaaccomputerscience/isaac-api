package uk.ac.cam.cl.dtg.segue.api;

import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

@Path("/recaptcha")
public class reCAPTCHAFacade {
    private final PropertiesLoader properties;
    private static final Logger log = LoggerFactory.getLogger(reCAPTCHAFacade.class);

    @Inject
    public reCAPTCHAFacade(PropertiesLoader properties) {
        Validate.notNull(properties.getProperty(GOOGLE_RECAPTCHA_SECRET));
        this.properties = properties;
    }

    @GET
    @Path("/verify")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isCaptchaValid(@QueryParam("response") String response) {
        if (response == null || response.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing reCAPTCHA response token.").build();
        }

        if (verifyRecaptcha(response)) {
            return Response.ok().entity("reCAPTCHA verification successful.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("reCAPTCHA verification failed.").build();
        }
    }

    private synchronized boolean verifyRecaptcha(String response) {
        try {
            String secretKey = properties.getProperty(GOOGLE_RECAPTCHA_SECRET);
            String url = "https://www.google.com/recaptcha/api/siteverify",
                    params = "secret=" + secretKey + "&response=" + response;

            HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
            http.setDoOutput(true);
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            OutputStream out = http.getOutputStream();
            out.write(params.getBytes("UTF-8"));
            out.flush();
            out.close();

            InputStream res = http.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(res, "UTF-8"));

            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            JSONObject json = new JSONObject(sb.toString());
            res.close();

            return json.getBoolean("success");
        } catch (Exception e) {
            log.error("Error during reCAPTCHA validation.", e);
        }
        return false;
    }
}
