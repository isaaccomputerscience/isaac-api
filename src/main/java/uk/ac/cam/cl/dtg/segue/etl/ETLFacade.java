package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Created by Ian on 17/10/2016.
 */
@Path("/etl")
@Tag(name = "/etl")
public class ETLFacade extends AbstractSegueFacade {
  private static final Logger log = LoggerFactory.getLogger(ETLFacade.class);

  private final ETLManager etlManager;

  private static final Pattern SHA_PATTERN = Pattern.compile("^[0-9a-f]{40}$");

  /**
   * Constructor that provides a properties loader.
   *
   * @param properties the propertiesLoader.
   * @param manager    the ETL Manager (for content)
   */
  @Inject
  public ETLFacade(final PropertiesLoader properties, final ETLManager manager) {
    super(properties, null);
    this.etlManager = manager;
  }

  @POST
  @Path("/set_version_alias/{alias}/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Update a content version alias.",
    description = "This is primarily used to set the 'live' content version."
  )
  public Response setLiveVersion(@PathParam("alias") final String alias, @PathParam("version") final String version) {
    if (!SHA_PATTERN.matcher(version).matches()) {
      log.error("Version did not match expected SHA format");
      return Response.serverError().entity("Version did not match expected SHA format").build();
    }

    try {
      etlManager.setNamedVersion(alias, version);
      log.info("Finished processing ETL request");
      return Response.ok().build();
    } catch (Exception e) {
      log.error("Failed to set alias version:" + e.getMessage());
      log.info("Finished processing ETL request");
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/ping")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Check the status of the ETL server.")
  public Response statusCheck() {
    return Response.ok().entity("{\"code\" : 200}").build();
  }
}
