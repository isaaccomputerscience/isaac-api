package uk.ac.cam.cl.dtg.segue.api;

import com.google.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

@Provider
@PreMatching
public class SessionValidator implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionValidator.class);

    private final UserAuthenticationManager userAuthenticationManager;
    private final PropertiesLoader properties;
    private final Integer sessionExpirySeconds;
    @Context private HttpServletRequest httpServletRequest;
    @Context private HttpServletResponse httpServletResponse;

    @Inject
    public SessionValidator(final UserAuthenticationManager userAuthenticationManager, PropertiesLoader properties) {
        log.info("Initialising session validator");
        this.userAuthenticationManager = userAuthenticationManager;
        this.properties = properties;
        this.sessionExpirySeconds = Integer.parseInt(properties.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        Cookie authCookie = containerRequestContext.getCookies().get(SEGUE_AUTH_COOKIE);
        if (authCookie != null) {
            if (!userAuthenticationManager.isSessionValid(httpServletRequest)) {
                log.warn("Request rejected");
                httpServletRequest.getSession().invalidate();
//                .cookie(userAuthenticationManager.createExpiringJakartaCookie())
                containerRequestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Session is invalid").build());
            }
            log.info("Request validated");
        }
        log.info("Anonymous request");
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Cookie authCookie = containerRequestContext.getCookies().get(SEGUE_AUTH_COOKIE);
        if (containerResponseContext.getCookies().get(SEGUE_AUTH_COOKIE) != null) {
            log.info(String.valueOf(containerResponseContext.getCookies().get(SEGUE_AUTH_COOKIE)));
        }
        if (authCookie != null) {
            SimpleDateFormat sessionDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, sessionExpirySeconds);
            String sessionExpiryDate = sessionDateFormat.format(calendar.getTime());

            Map<String, String> sessionInformation = userAuthenticationManager.decodeCookie(authCookie);
            sessionInformation.replace(DATE_EXPIRES, sessionExpiryDate);
            String updatedHMAC = userAuthenticationManager.calculateUpdatedHMAC(sessionInformation);
            sessionInformation.put(HMAC, updatedHMAC);

            jakarta.servlet.http.Cookie newAuthCookie = userAuthenticationManager.createAuthNewCookie(sessionInformation, sessionExpirySeconds);
            httpServletResponse.addCookie(newAuthCookie);
        }

    }
}
