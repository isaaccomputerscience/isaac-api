package uk.ac.cam.cl.dtg.segue.api;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.jboss.resteasy.core.interception.jaxrs.ContainerResponseContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

@Provider
@PreMatching
public class SessionValidator implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionValidator.class);

    private final UserAuthenticationManager userAuthenticationManager;
    private final PropertiesLoader properties;
    private final Integer sessionExpirySeconds;
    @Context
    private HttpServletRequest httpServletRequest;
    @Context
    private HttpServletResponse httpServletResponse;

    @Inject
    public SessionValidator(final UserAuthenticationManager userAuthenticationManager, PropertiesLoader properties) {
        this.userAuthenticationManager = userAuthenticationManager;
        this.properties = properties;
        this.sessionExpirySeconds = Integer.parseInt(this.properties.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        Cookie authCookie = containerRequestContext.getCookies().get(SEGUE_AUTH_COOKIE);
        if (authCookie != null) {
            if (!userAuthenticationManager.isSessionValid(httpServletRequest)) {
                log.warn("Request made with invalid segue auth cookie - closing session");
                httpServletRequest.getSession().invalidate();
                containerRequestContext.abortWith(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("Authentication cookie is invalid")
                        .cookie(userAuthenticationManager.createAuthLogoutNewCookie())
                        .build()
                );
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Cookie authCookie = containerRequestContext.getCookies().get(SEGUE_AUTH_COOKIE);
        if (authCookie != null && !isLogoutCookiePresent(httpServletResponse) && wasRequestValid(containerResponseContext)) {
            try {
                jakarta.servlet.http.Cookie newAuthCookie = generateRefreshedSegueAuthCookie(authCookie);
                httpServletResponse.addCookie(newAuthCookie);
            } catch (JsonProcessingException e) {
                log.error("Unable to save cookie.", e);
            }
        }
    }

    private static boolean isLogoutCookiePresent(HttpServletResponse response) {
        ArrayList<String> cookies = (ArrayList<String>) response.getHeaders("Set-Cookie");
        return cookies.stream().anyMatch(cookie -> cookie.contains(SEGUE_AUTH_COOKIE) && cookie.contains("Max-Age=0"));
    }

    private static boolean wasRequestValid(ContainerResponseContext containerResponseContext) {
        return ((ContainerResponseContextImpl) containerResponseContext).getJaxrsResponse().getStatus() == Response.Status.OK.getStatusCode();
    }

    private jakarta.servlet.http.Cookie generateRefreshedSegueAuthCookie(Cookie authCookie) throws IOException {
        Map<String, String> sessionInformation = userAuthenticationManager.decodeCookie(authCookie);
        String sessionExpiryDate = getFutureDateString(sessionExpirySeconds);
        sessionInformation.put(DATE_EXPIRES, sessionExpiryDate);
        String updatedHMAC = userAuthenticationManager.calculateUpdatedHMAC(sessionInformation);
        sessionInformation.put(HMAC, updatedHMAC);

        jakarta.servlet.http.Cookie newAuthCookie = userAuthenticationManager.createAuthCookie(sessionInformation, sessionExpirySeconds);
        return newAuthCookie;
    }

    private static String getFutureDateString(Integer secondsinFuture) {
        SimpleDateFormat sessionDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, secondsinFuture);
        return sessionDateFormat.format(calendar.getTime());
    }
}
