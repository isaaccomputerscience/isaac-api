/**
 * Copyright 2022 Matthew Trew
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static com.google.common.collect.Maps.immutableEntry;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.GAMEBOARD_MAX_TITLE_LENGTH;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.GAME_BOARD_TARGET_SIZE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.HIDE_FROM_FILTER_TAG;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.RELATED_CONTENT_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.WILDCARD_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEPRECATED_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DIFFICULTY_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EXAM_BOARD_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LEVEL_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.STAGE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager.BooleanSearchClause;

public class GameManagerTest {
  private GitContentManager dummyContentManager;
  private GameboardPersistenceManager dummyGameboardPersistenceManager;
  private MapperFacade dummyMapper;
  private QuestionManager dummyQuestionManager;

  @Before
  public void setUp() {
    this.dummyContentManager = createMock(GitContentManager.class);
    this.dummyGameboardPersistenceManager = createMock(GameboardPersistenceManager.class);
    this.dummyMapper = createMock(MapperFacade.class);
    this.dummyQuestionManager = createMock(QuestionManager.class);
  }

  @Test
  public void getNextQuestionsForFilter_appliesExclusionFilterForDeprecatedQuestions() throws
      ContentManagerException {

    // Arrange
    GameManager gameManager = new GameManager(
        this.dummyContentManager,
        this.dummyGameboardPersistenceManager,
        this.dummyMapper,
        this.dummyQuestionManager,
        "latest"
    );

    // configure the mock GitContentManager to record the filters that are sent to it by getNextQuestionsForFilter()
    Capture<List<BooleanSearchClause>> capturedFilters = Capture.newInstance();
    expect(dummyContentManager.findByFieldNamesRandomOrder(
        capture(capturedFilters),
        anyInt(),
        anyInt(),
        anyLong())
    ).andStubReturn(new ResultsWrapper<>());
    replay(dummyContentManager);

    // Act
    gameManager.getNextQuestionsForFilter(new GameFilter(), 1, 1L);

    // Assert
    // check that one of the filters sent to GitContentManager was the deprecated question exclusion filter
    List<BooleanSearchClause> filters = capturedFilters.getValues().get(0);
    BooleanSearchClause deprecatedFilter = filters.stream()
        .filter(f -> Objects.equals(f.getField(), "deprecated")).collect(Collectors.toList()).get(0);

    assertNotNull(deprecatedFilter);
    assertEquals(deprecatedFilter.getOperator(), Constants.BooleanOperator.NOT);
    assertEquals(deprecatedFilter.getValues(), Collections.singletonList("true"));
  }

  @Test
  public void generateRandomQuestions_returnsCorrectNumberOfQuestions() throws ContentManagerException {

    // Arrange
    int limit = 5;

    var gameManager = new GameManager(
        this.dummyContentManager,
        this.dummyGameboardPersistenceManager,
        this.dummyMapper,
        this.dummyQuestionManager,
        "latest"
    );

    List<IsaacQuestionPageDTO> questions = new ArrayList<>();
    for (int i = 0; i < limit; i++) {
      IsaacQuestionPageDTO question = createMock(IsaacQuestionPageDTO.class);
      question.setSupersededBy(null);
      questions.add(question);
    }
    var resultsWrapper = new ResultsWrapper(questions, (long) limit);

    // Create a ResultsWrapper with a specific number of questions
    expect(dummyContentManager.findByFieldNamesRandomOrder(
        anyObject(),
        anyInt(),
        anyInt(),
        anyLong())
    ).andReturn(resultsWrapper)
        .times(1);
    replay(dummyContentManager);

    // Act
    var result = gameManager.generateRandomQuestions(new GameFilter(), limit);

    // Assert
    // Check that the result has the correct number of questions
    assertEquals(limit, result.size());
  }

  @Test
  public void generateRandomQuestions_appliesExclusionFilterForSupersededQuestions() throws ContentManagerException {

    // Arrange
    int limit = 5;
    List<IsaacQuestionPageDTO> questions = new ArrayList<>();
    for (int i = 0; i < limit; i++) {
      IsaacQuestionPageDTO question = createMock(IsaacQuestionPageDTO.class);
      questions.add(question);
    }
    questions.get(1).setSupersededBy("test_string");
    var resultsWrapper = new ResultsWrapper(questions, (long) limit);

    // Create a ResultsWrapper with a specific number of questions
    expect(dummyContentManager.findByFieldNamesRandomOrder(
        anyObject(),
        anyInt(),
        anyInt(),
        anyLong())
    ).andReturn(resultsWrapper)
        .times(2);
    replay(dummyContentManager);

    var gameManager = new GameManager(
        this.dummyContentManager,
        this.dummyGameboardPersistenceManager,
        this.dummyMapper,
        this.dummyQuestionManager,
        "latest"
    );

    // Act
    var result = gameManager.generateRandomQuestions(new GameFilter(), limit);

    // Assert
    // Check that the result has the correct number of questions
    assertEquals(limit, result.size());
  }
}
