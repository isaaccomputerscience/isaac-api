package uk.ac.cam.cl.dtg.segue.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class DiagnosticFilter implements ContainerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(DiagnosticFilter.class);
  private static boolean logged = false;

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    if (!logged) {
      logged = true;
      try {
        Class<?> uriComplianceClass = Class.forName("org.eclipse.jetty.http.UriCompliance");
        log.info("MMM_DIAGNOSTIC UriCompliance class found: {}", uriComplianceClass.getCanonicalName());
      } catch (ClassNotFoundException e) {
        log.warn("MMM_DIAGNOSTIC UriCompliance class not found");
      }
    }
  }
}