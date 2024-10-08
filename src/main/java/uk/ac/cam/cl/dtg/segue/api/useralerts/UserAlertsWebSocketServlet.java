package uk.ac.cam.cl.dtg.segue.api.useralerts;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.util.RequestIpExtractor.getClientIpAddr;

import com.google.inject.Injector;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.jetty.ee9.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee9.websocket.server.JettyWebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueContextNotifier;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Created by du220 on 17/07/2017.
 */

@WebServlet(name = "UserAlertsWebSocketServlet", urlPatterns = {"/api/user-alerts/*"})
public class UserAlertsWebSocketServlet extends JettyWebSocketServlet {

  private static final Logger log = LoggerFactory.getLogger(UserAlertsWebSocketServlet.class);
  private static final int BAD_REQUEST = 400;
  private static final int FORBIDDEN = 403;
  private static final Injector INJECTOR = SegueGuiceConfigurationModule.getGuiceInjector();
  private final String hostName = INJECTOR.getInstance(PropertiesLoader.class).getProperty(HOST_NAME);

  @Override
  public void configure(final JettyWebSocketServletFactory factory) {

    factory.setCreator((servletUpgradeRequest, servletUpgradeResponse) -> SegueContextNotifier.getInjector()
        .getInstance(UserAlertsWebSocket.class));

  }

  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    // We have been seeing malformed WebSocket requests. Add some debug logging to these:
    if (!"websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
      log.debug("WebSocket Upgrade request from {} has incorrect header 'Upgrade: {}', headers: {}, 'Via: {}'.",
          getClientIpAddr(request), request.getHeader("Upgrade"), Collections.list(request.getHeaderNames()),
          request.getHeader("Via"));
    }
    if (null == request.getHeader("Sec-WebSocket-Key")) {
      log.warn("WebSocket Upgrade request from {} has missing 'Sec-WebSocket-Key' header."
              + " 'Sec-WebSocket-Extensions: {}', 'Sec-WebSocket-Version: {}', 'User-Agent: {}'",
          getClientIpAddr(request), request.getHeader("Sec-WebSocket-Extensions"),
          request.getHeader("Sec-WebSocket-Version"), request.getHeader("User-Agent"));
      response.setStatus(BAD_REQUEST);
      return;
    }

    // WebSockets are not protected by CORS, so we must check the origin explicitly here:
    String origin = request.getHeader("Origin");
    if (!hostName.contains("localhost") && (null == origin || !origin.equals("https://" + hostName))) {
      // If we have no origin, or an origin not matching the current hostname; abort the Upgrade request with
      // a HTTP Forbidden. Allow an API running on localhost to bypass these origin checks.
      log.warn("WebSocket Upgrade request has unexpected Origin: '{}'. Blocking access to: {}", origin,
          request.getServletPath());
      response.setStatus(FORBIDDEN);
      return;
    }
    super.service(request, response);
  }
}
