package uk.ac.cam.cl.dtg.segue.api.managers;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.GOOGLE_RECAPTCHA_SECRET;

public class reCAPTCHAManagerTest {
    private PropertiesLoader properties;
    private reCAPTCHAManager reCAPTCHAManagerUnderTest;

    @Before
    public final void setUp() throws Exception {
        properties = createMock(PropertiesLoader.class);
        expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("secret-key").atLeastOnce();
        replay(properties);

        reCAPTCHAManagerUnderTest = new reCAPTCHAManager(properties) {
            @Override
            protected JSONObject performHttpRequest(String url, String params) throws Exception {
                return new JSONObject("{\"success\": true}");
            }
        };
    }

    @Test
    public final void verifyRecaptcha_ValidResponse_ShouldReturnSuccess() throws Exception {
        // Execute the method
        boolean result = reCAPTCHAManagerUnderTest.verifyRecaptcha("valid-response");

        // Verify the results
        assertTrue(result);
    }

    @Test
    public final void isCaptchaValid_ValidResponse_ShouldReturnSuccessMessage() throws Exception {
        // Execute the method
        String result = reCAPTCHAManagerUnderTest.isCaptchaValid("valid-response");

        // Verify the results
        assertEquals("reCAPTCHA verification successful.", result);
    }

    @Test
    public final void verifyRecaptcha_InvalidResponse_ShouldReturnSuccess() throws Exception {
        reCAPTCHAManagerUnderTest = new reCAPTCHAManager(properties) {
            @Override
            protected JSONObject performHttpRequest(String url, String params) throws Exception {
                return new JSONObject("{\"success\": false}");
            }
        };
        // Execute the method
        boolean result = reCAPTCHAManagerUnderTest.verifyRecaptcha("invalid-response");

        // Verify the results
        assertFalse(result);
    }

    @Test
    public final void isCaptchaValid_InvalidResponse_ShouldReturnFailedMessage() throws Exception {
        reCAPTCHAManagerUnderTest = new reCAPTCHAManager(properties) {
            @Override
            protected JSONObject performHttpRequest(String url, String params) throws Exception {
                return new JSONObject("{\"success\": false}");
            }
        };
        // Execute the method
        String result = reCAPTCHAManagerUnderTest.isCaptchaValid("invalid-response");

        // Verify the results
        assertEquals("reCAPTCHA verification failed.", result);
    }

    @Test
    public final void verifyRecaptcha_EmptyResponse_ShouldReturnFailed() throws Exception {
        // Execute the method
        boolean result = reCAPTCHAManagerUnderTest.verifyRecaptcha("");

        // Verify the results
        assertFalse(result);
    }

    @Test
    public final void isCaptchaValid_EmptyResponse_ShouldReturnMissingResponseMessage() throws Exception {
        // Execute the method
        String result = reCAPTCHAManagerUnderTest.isCaptchaValid("");

        // Verify the results
        assertEquals("Missing reCAPTCHA response token.", result);
    }

}
