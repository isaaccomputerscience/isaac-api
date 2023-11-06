package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class QuizManagerTest extends AbstractManagerTest {

  private QuizManager quizManager;
  private PropertiesLoader properties;
  private IsaacQuizDTO brokenQuiz;

  @Before
  public void setUp() {
    properties = createMock(PropertiesLoader.class);

    ContentService contentService = createMock(ContentService.class);
    GitContentManager contentManager = createMock(GitContentManager.class);
    ContentSummarizerService contentSummarizerService = createMock(ContentSummarizerService.class);
    ContentMapper mapper = createMock(ContentMapper.class);
    quizManager = new QuizManager(properties, contentService, contentManager, contentSummarizerService, mapper);

    brokenQuiz = new IsaacQuizDTO();
    brokenQuiz.setChildren(ImmutableList.of(quizSection1, new ContentDTO(), quizSection2));

    replay(properties);
  }

  @Test
  public void extractSectionObjectsInDev() throws ContentManagerException {
    withMock(properties, m ->
        expect(m.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andStubReturn(Constants.EnvironmentType.DEV.name())
    );

    List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(studentQuiz);
    assertCorrectSections(sections);

    assertThrows(ContentManagerException.class, () -> quizManager.extractSectionObjects(brokenQuiz));
  }

  @Test
  public void extractSectionObjectsInProd() throws ContentManagerException {
    withMock(properties, m ->
        expect(m.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andStubReturn(Constants.EnvironmentType.PROD.name())
    );

    List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(studentQuiz);
    assertCorrectSections(sections);

    sections = quizManager.extractSectionObjects(brokenQuiz);
    assertCorrectSections(sections);
  }

  private void assertCorrectSections(List<IsaacQuizSectionDTO> sections) {
    assertEquals(ImmutableList.of(quizSection1, quizSection2), sections);
  }
}
