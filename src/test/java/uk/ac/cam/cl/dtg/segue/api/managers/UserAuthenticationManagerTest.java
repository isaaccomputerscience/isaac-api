package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class UserAuthenticationManagerTest {
    private UserAuthenticationManager userAuthenticationManager;
    private IUserDataManager dummyDatabase;
    private PropertiesLoader dummyPropertiesLoader;
    private Map<AuthenticationProvider, IAuthenticator> dummyProvidersMap;
    private EmailManager dummyQueue;
    private String dummyHMACSalt;
    private String dummyHostName;

    @BeforeEach
    public void BeforeEach() {
        this.dummyDatabase = createMock(IUserDataManager.class);
        this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
        this.dummyProvidersMap = new HashMap<>();

        this.dummyHMACSalt = "BOB";
        this.dummyHostName = "bob";

        expect(this.dummyPropertiesLoader.getProperty(HMAC_SALT)).andReturn(dummyHMACSalt).anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(HOST_NAME)).andReturn(dummyHostName).anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT)).andReturn("60")
                .anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(SESSION_EXPIRY_SECONDS_REMEMBERED)).andReturn("360")
                .anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andReturn("DEV").anyTimes();
        replay(this.dummyPropertiesLoader);

        userAuthenticationManager = new UserAuthenticationManager(dummyDatabase, dummyPropertiesLoader, dummyProvidersMap, dummyQueue);
    }

    @Test
    public void isSessionValid_valid() throws JsonProcessingException, SegueDatabaseException {
        Date cookieExpiryDate = Date.from(Instant.now().plus(300, SECONDS));
        String cookieExpiryDateString = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(cookieExpiryDate);
        Map<String, String> sessionInformation = new HashMap<>(4);
        sessionInformation.put(SESSION_USER_ID, "1");
        sessionInformation.put(SESSION_TOKEN, "1");
        sessionInformation.put(DATE_EXPIRES, cookieExpiryDateString);
        sessionInformation.put(HMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));
        Cookie segueAuthCookie = userAuthenticationManager.createAuthCookie(sessionInformation, 300);

        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getCookies()).andReturn(new Cookie[]{segueAuthCookie}).anyTimes();
        replay(mockRequest);

        RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
        expect(mockUser.getSessionToken()).andReturn(1);
        replay(mockUser);

        expect(dummyDatabase.getById(1L)).andReturn(mockUser);
        replay(dummyDatabase);

        assertTrue(userAuthenticationManager.isSessionValid(mockRequest));
    }

    @Test
    public void isSessionValid_noCookies() throws SegueDatabaseException {
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getCookies()).andReturn(null).anyTimes();
        replay(mockRequest);

        RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
        expect(mockUser.getSessionToken()).andReturn(1);
        replay(mockUser);

        expect(dummyDatabase.getById(1L)).andReturn(mockUser);
        replay(dummyDatabase);

        assertFalse(userAuthenticationManager.isSessionValid(mockRequest));
    }

    @Test
    public void isSessionValid_noAuthCookie() throws SegueDatabaseException {
        Cookie notAuthCookie = new Cookie("NOT_AUTH_COOKIE", "");
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getCookies()).andReturn(new Cookie[]{notAuthCookie}).anyTimes();
        replay(mockRequest);

        RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
        expect(mockUser.getSessionToken()).andReturn(1);
        replay(mockUser);

        expect(dummyDatabase.getById(1L)).andReturn(mockUser);
        replay(dummyDatabase);

        assertFalse(userAuthenticationManager.isSessionValid(mockRequest));
    }

    @Test
    public void calculateUpdatedHMAC_noPartialLogin() {
        String expectedHMAC = "lgskryXMwRF+Ef/jiVp4LQX2g66JoUlFg/ixxjYZIrs=";
        Date cookieExpiryDate = Date.from(LocalDateTime.of(2020, 1, 1, 0, 0,0).toInstant(UTC));
        String cookieExpiryDateString = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(cookieExpiryDate);
        Map<String, String> sessionInformation = Map.of(
                SESSION_USER_ID, "1",
                SESSION_TOKEN, "1",
                DATE_EXPIRES, ""
        );

        assertEquals(expectedHMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));
    }

    @Test
    public void calculateUpdatedHMAC_PartialLogin() {
        String expectedHMAC = "t3joONtilIM07Or8/2TjXf+IamSB5xrzFphjnM5Pi9k=";
        Date cookieExpiryDate = Date.from(LocalDateTime.of(2020, 1, 1, 0, 0,0).toInstant(UTC));
        String cookieExpiryDateString = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(cookieExpiryDate);
        Map<String, String> sessionInformation = Map.of(
                SESSION_USER_ID, "1",
                SESSION_TOKEN, "1",
                DATE_EXPIRES, "",
                PARTIAL_LOGIN_FLAG, "true"
        );

        assertEquals(expectedHMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));
    }
}