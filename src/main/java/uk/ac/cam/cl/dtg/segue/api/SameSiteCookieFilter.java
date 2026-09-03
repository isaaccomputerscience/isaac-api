package uk.ac.cam.cl.dtg.segue.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

@Provider
public class SameSiteCookieFilter implements ContainerResponseFilter {

  private static final String SAME_SITE_NONE = "__SAME_SITE_NONE__";
  private static final String SAME_SITE_LAX = "__SAME_SITE_LAX__";

  @Override
  public void filter(final ContainerRequestContext requestContext,
                     final ContainerResponseContext responseContext) {

    List<Object> setCookieHeaders = responseContext.getHeaders().get("Set-Cookie");
    if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
      return;
    }

    // Entries may be raw header Strings or typed objects (e.g. NewCookie, added via
    // Response.cookie(...)) - toString() renders either as a valid Set-Cookie header value.
    List<Object> updatedHeaders = new ArrayList<>();
    for (Object cookieHeader : setCookieHeaders) {
      updatedHeaders.add(addSameSiteAttribute(cookieHeader.toString()));
    }

    responseContext.getHeaders().put("Set-Cookie", updatedHeaders);
  }

  private String addSameSiteAttribute(final String cookieHeader) {
    if (cookieHeader.contains(SAME_SITE_NONE)) {
      return cookieHeader.replace(SAME_SITE_NONE, "").trim() + "; SameSite=None";
    } else if (cookieHeader.contains(SAME_SITE_LAX)) {
      return cookieHeader.replace(SAME_SITE_LAX, "").trim() + "; SameSite=Lax";
    }
    return cookieHeader;
  }
}
