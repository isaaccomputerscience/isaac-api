package uk.ac.cam.cl.dtg.segue.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class GlossaryFacadeTest {

  private GlossaryFacade glossaryFacade;

  @BeforeEach
  void beforeEach() {
    PropertiesLoader properties = createMock(PropertiesLoader.class);
    GitContentManager contentManager = createMock(GitContentManager.class);
    ILogManager logManager = createMock(ILogManager.class);
    replay(properties, contentManager, logManager);
    glossaryFacade = new GlossaryFacade(properties, contentManager, "test-index", logManager);
  }

  @Test
  void getTerms_nonNumericLimit_returnsBadRequest() {
    Response response = glossaryFacade.getTerms("0", "abc");

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void getTerms_nonNumericStartIndex_returnsBadRequest() {
    Response response = glossaryFacade.getTerms("abc", "10");

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
}
