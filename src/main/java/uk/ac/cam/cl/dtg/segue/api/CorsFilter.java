package uk.ac.cam.cl.dtg.segue.api;

import com.google.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * CORS Filter for ALB migration.
 * Emits CORS headers from the application instead of relying on nginx ingress annotations.
 * This allows the API to work with AWS ALB which doesn't have a built-in CORS module.
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final String DEFAULT_ALLOWED_ORIGINS = "https://*.isaaccomputerscience.org";

  private final String allowedOrigins;

  @Inject
  public CorsFilter() {
    this.allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
      requestContext.abortWith(
          Response.ok()
              .header("Access-Control-Allow-Origin", getAllowedOrigin(requestContext))
              .header("Access-Control-Allow-Methods",
                  "GET, POST, PUT, DELETE, OPTIONS, PATCH")
              .header("Access-Control-Allow-Headers",
                  "Origin, X-Requested-With, Content-Type, Accept, Authorization, X-Api-Token")
              .header("Access-Control-Allow-Credentials", "true")
              .header("Access-Control-Max-Age", "3600")
              .build());
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext,
                     final ContainerResponseContext responseContext) throws IOException {
    if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
      // Request filter already set all headers via abortWith(). Running here too would duplicate them,
      // which causes Safari/mobile to fail the CORS preflight check.
      return;
    }
    String allowedOrigin = getAllowedOrigin(requestContext);
    responseContext.getHeaders().add("Access-Control-Allow-Origin", allowedOrigin);
    responseContext.getHeaders().add("Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS, PATCH");
    responseContext.getHeaders().add("Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Authorization, X-Api-Token");
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
  }

  /**
   * Validate and return the allowed origin from the request.
   * Currently, allows all requests from Isaac domains (*.isaaccomputerscience.org).
   *
   * @param requestContext the request context
   * @return the allowed origin, or * if validation fails
   */
  private String getAllowedOrigin(final ContainerRequestContext requestContext) {
    String origin = requestContext.getHeaderString("Origin");

    if (origin == null) {
      return allowedOrigins;
    }

    // The allowedOrigins property can be configured as a regex or comma-separated list if needed.
    if (origin.contains("isaaccomputerscience.org") || origin.contains("localhost")) {
      return origin;
    }

    return allowedOrigins;
  }
}