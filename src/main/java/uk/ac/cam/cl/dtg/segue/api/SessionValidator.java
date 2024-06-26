package uk.ac.cam.cl.dtg.segue.api;

import static java.time.ZoneOffset.UTC;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DATE_EXPIRES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC;
import static uk.ac.cam.cl.dtg.segue.api.Constants.PARTIAL_LOGIN_FLAG;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_AUTH_COOKIE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_EXPIRY_SECONDS_DEFAULT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_EXPIRY_SECONDS_FALLBACK;

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
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import org.jboss.resteasy.core.interception.jaxrs.ContainerResponseContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

@Provider
@PreMatching
public class SessionValidator implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(SessionValidator.class);

  private final UserAuthenticationManager userAuthenticationManager;
  private final Integer sessionExpirySeconds;
  @Context
  private HttpServletRequest httpServletRequest;
  @Context
  private HttpServletResponse httpServletResponse;

  @Inject
  public SessionValidator(final UserAuthenticationManager userAuthenticationManager,
                          final PropertiesLoader properties) {
    this.userAuthenticationManager = userAuthenticationManager;
    this.sessionExpirySeconds = properties.getIntegerPropertyOrFallback(
        SESSION_EXPIRY_SECONDS_DEFAULT, SESSION_EXPIRY_SECONDS_FALLBACK);
  }

  @Override
  public void filter(final ContainerRequestContext containerRequestContext) {
    Cookie authCookie = containerRequestContext.getCookies().get(SEGUE_AUTH_COOKIE);
    if (authCookie != null && !userAuthenticationManager.isSessionValid(httpServletRequest)) {
      log.warn("Request made with invalid segue auth cookie - closing session");
      invalidateSession();
      containerRequestContext.abortWith(Response
          .status(Response.Status.BAD_REQUEST)
          .entity("Authentication cookie is invalid")
          .cookie(userAuthenticationManager.createAuthLogoutNewCookie())
          .build()
      );
    }
  }

  @Override
  public void filter(final ContainerRequestContext containerRequestContext,
                     final ContainerResponseContext containerResponseContext) {
    if (!isLogoutCookiePresent(httpServletResponse)) {
      Cookie authCookie = containerRequestContext.getCookies().get(SEGUE_AUTH_COOKIE);
      try {
        if (authCookie != null && !isPartialLoginCookie(authCookie) && wasRequestValid(containerResponseContext)) {
          jakarta.servlet.http.Cookie newAuthCookie = generateRefreshedSegueAuthCookie(authCookie);
          httpServletResponse.addCookie(newAuthCookie);
        }
      } catch (IOException e) {
        log.error("Failed to parse an auth cookie for refresh", e);
      }
    }
  }

  private void invalidateSession() {
    httpServletRequest.getSession().invalidate();
    try {
      userAuthenticationManager.invalidateSessionToken(httpServletRequest);
    } catch (NoUserLoggedInException e) {
      log.error("Auth cookie is missing a user");
    } catch (SegueDatabaseException e) {
      log.error("Database error while invalidating session token");
    }
  }

  private static boolean isLogoutCookiePresent(final HttpServletResponse response) {
    Collection<String> cookies = response.getHeaders("Set-Cookie");
    return cookies.stream().anyMatch(cookie -> cookie.contains(SEGUE_AUTH_COOKIE) && cookie.contains("Max-Age=0"));
  }

  private boolean isPartialLoginCookie(final Cookie authCookie) throws IOException {
    Map<String, String> sessionInformation = userAuthenticationManager.decodeCookie(authCookie);
    String partialLoginFlag = sessionInformation.get(PARTIAL_LOGIN_FLAG);
    return partialLoginFlag != null && partialLoginFlag.equals(String.valueOf(true));
  }

  private static boolean wasRequestValid(final ContainerResponseContext containerResponseContext) {
    return ((ContainerResponseContextImpl) containerResponseContext).getJaxrsResponse().getStatus()
        == Response.Status.OK.getStatusCode();
  }

  private jakarta.servlet.http.Cookie generateRefreshedSegueAuthCookie(final Cookie authCookie) throws IOException {
    Map<String, String> sessionInformation = userAuthenticationManager.decodeCookie(authCookie);
    String sessionExpiryDate = getFutureDateString(sessionExpirySeconds);
    sessionInformation.put(DATE_EXPIRES, sessionExpiryDate);
    String updatedHMAC = userAuthenticationManager.calculateUpdatedHMAC(sessionInformation);
    sessionInformation.put(HMAC, updatedHMAC);

    return userAuthenticationManager.createAuthCookie(sessionInformation, sessionExpirySeconds);
  }

  private static String getFutureDateString(final Integer secondsInFuture) {
    DateTimeFormatter sessionDateFormat = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).withZone(UTC);
    return sessionDateFormat.format(Instant.now().plusSeconds(secondsInFuture));
  }
}
