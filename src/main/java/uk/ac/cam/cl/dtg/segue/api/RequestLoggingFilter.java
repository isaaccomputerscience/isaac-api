package uk.ac.cam.cl.dtg.segue.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    String method = requestContext.getMethod();
    String uri = requestContext.getUriInfo().getRequestUri().toString();
    String path = requestContext.getUriInfo().getPath();

    if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH")
        || path.contains("question") || path.contains("answer")) {
      log.info("MMM_REQUEST method={} path={} fullUri={}", method, path, uri);
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
      throws IOException {
    if (responseContext.getStatus() >= 400) {
      String method = requestContext.getMethod();
      String path = requestContext.getUriInfo().getPath();
      int status = responseContext.getStatus();
      log.warn("MMM_ERROR_RESPONSE status={} method={} path={}", status, method, path);
    }
  }
}
