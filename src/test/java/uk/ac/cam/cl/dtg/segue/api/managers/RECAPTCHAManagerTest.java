package uk.ac.cam.cl.dtg.segue.api.managers;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.GOOGLE_RECAPTCHA_SECRET;

public class RECAPTCHAManagerTest {
    private static final String SUCCESS_RESPONSE_JSON = "{\"success\": true}";
    private static final String FAILURE_RESPONSE_JSON = "{\"success\": false}";

    private PropertiesLoader properties;

    @Before
    public final void setUp() throws Exception {
        properties = createMock(PropertiesLoader.class);
        expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe").atLeastOnce();
        replay(properties);
    }

    private void setSecretInvalid() throws Exception {
        properties = createMock(PropertiesLoader.class);
        expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("invalid-secret").atLeastOnce();
        replay(properties);
    }

    private RECAPTCHAManager createTestInstance(String responseJson) {
        return new RECAPTCHAManager(properties) {
            @Override
            protected JSONObject performHttpRequest(String url, String params) {
                return new JSONObject(responseJson);
            }
        };
    }

    @Test
    public final void verifyRecaptcha_ValidResponse_ShouldReturnSuccess() throws Exception {
        RECAPTCHAManager testInstance = createTestInstance(SUCCESS_RESPONSE_JSON);
        assertTrue(testInstance.verifyRecaptcha("valid-response"));
    }

    @Test
    public final void isCaptchaValid_ValidResponse_ShouldReturnSuccessMessage() throws Exception {
        RECAPTCHAManager testInstance = createTestInstance(SUCCESS_RESPONSE_JSON);
        assertEquals("reCAPTCHA verification successful.", testInstance.recaptchaResultString("valid-response"));
    }

    @Test
    public final void verifyRecaptcha_InvalidResponse_ShouldReturnFailure() throws Exception {
        setSecretInvalid();
        RECAPTCHAManager testInstance = createTestInstance(FAILURE_RESPONSE_JSON);
        assertFalse(testInstance.verifyRecaptcha("invalid-response"));
    }

    @Test
    public final void isCaptchaValid_InvalidResponse_ShouldReturnFailedMessage() throws Exception {
        setSecretInvalid();
        RECAPTCHAManager testInstance = createTestInstance(FAILURE_RESPONSE_JSON);
        assertEquals("reCAPTCHA verification failed.", testInstance.recaptchaResultString("invalid-response"));
    }

    @Test
    public final void verifyRecaptcha_EmptyResponse_ShouldReturnFailure() throws Exception {
        RECAPTCHAManager testInstance = createTestInstance(FAILURE_RESPONSE_JSON);
        assertFalse(testInstance.verifyRecaptcha(""));
    }

    @Test
    public final void isCaptchaValid_EmptyResponse_ShouldReturnMissingResponseMessage() throws Exception {
        RECAPTCHAManager testInstance = createTestInstance(FAILURE_RESPONSE_JSON);
        assertEquals("Missing reCAPTCHA response token.", testInstance.recaptchaResultString(""));
    }
}
