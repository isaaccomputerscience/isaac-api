package uk.ac.cam.cl.dtg.util.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.elasticsearch.core.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.RoleRequirement;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.content.AnvilApp;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.AnvilAppDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;

class MapStructContentMapperTest {
  private final MapStructContentMapper mapper = MapStructContentMapper.INSTANCE;

  private final ObjectMapper jsonMapper = new ObjectMapper();

  @ParameterizedTest
  @MethodSource("testCasesDOtoDTO")
  <S extends Content, T extends ContentDTO> void mappingDOReturnsExpectedDTO(S source, T expected)
      throws JsonProcessingException {
    S sourceWithCommonProperties = setOriginalCommonContentProperties(source);
    T expectedWithCommonProperties = setMappedCommonContentDTOProperties(expected);
    ContentDTO actual = (ContentDTO) mapper.map(sourceWithCommonProperties);
    assertEquals(expected.getClass(), actual.getClass());
    assertEquals(jsonMapper.writeValueAsString(expectedWithCommonProperties), jsonMapper.writeValueAsString(actual));
  }

  @ParameterizedTest
  @MethodSource("testCasesDTOtoDO")
  <S extends ContentDTO, T extends Content> void mappingDTOReturnsExpectedDO(S source, T expected)
      throws JsonProcessingException {
    S sourceWithCommonProperties = setOriginalCommonContentDTOProperties(source);
    T expectedWithCommonProperties = setMappedCommonContentProperties(expected);
    Content actual = (Content) mapper.map(sourceWithCommonProperties);
    assertEquals(expected.getClass(), actual.getClass());
    assertEquals(jsonMapper.writeValueAsString(expectedWithCommonProperties), jsonMapper.writeValueAsString(actual));
  }

  private static Stream<Arguments> testCasesDOtoDTO() {
    return Stream.of(
        Arguments.of(new Content(), new ContentDTO()),
        Arguments.of(prepareAnvilAppDO(), prepareAnvilAppDTO()),
        Arguments.of(prepareOriginalChoiceDO(), prepareChoiceDTO()),
        Arguments.of(prepareOriginalChoiceQuestionDO(new ChoiceQuestion()), prepareMappedChoiceQuestionDTO(new ChoiceQuestionDTO())),
        Arguments.of(prepareOriginalChoiceQuestionDO(new IsaacFreeTextQuestion()), prepareMappedChoiceQuestionDTO(new IsaacFreeTextQuestionDTO())),
        Arguments.of(prepareOriginalQuestionDO(), prepareMappedQuestionDTO())
    );
  }

  private static Stream<Arguments> testCasesDTOtoDO() {
    return Stream.of(
        Arguments.of(new ContentDTO(), new Content()),
        Arguments.of(prepareAnvilAppDTO(), prepareAnvilAppDO()),
        Arguments.of(prepareChoiceDTO(), prepareMappedChoiceDO()),
        Arguments.of(prepareOriginalChoiceQuestionDTO(new ChoiceQuestionDTO()), prepareMappedChoiceQuestionDO(new ChoiceQuestion())),
        Arguments.of(prepareOriginalChoiceQuestionDTO(new IsaacFreeTextQuestionDTO()), prepareMappedChoiceQuestionDO(new IsaacFreeTextQuestion())),
        Arguments.of(prepareOriginalQuestionDTO(), prepareMappedQuestionDO())
    );
  }

  private static <S extends Content> S setOriginalCommonContentProperties(S source) {
    // Set ContentBase properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    source.setId("id");
    source.setType("type");
    source.setTags(Set.of("tag1", "tag2"));
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set Content properties
    Content childContent1 = new Content();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    Content childContent2 = new Content();
    childContent2.setId("child2");
    childContent2.setPublished(true);

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of("relatedId1", "relatedId2"));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    source.setSearchableContent("searchable");
    source.setExpandable(false);

    return source;
  }

  private static <S extends Content> S setMappedCommonContentProperties(S source) {
    // Set ContentBase properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    source.setId("id");
    source.setType("type");
    source.setTags(Set.of("tag1", "tag2"));
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set Content properties
    Content childContent1 = new Content();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    childContent1.setTags(Set.of());
    Content childContent2 = new Content();
    childContent2.setId("child2");
    childContent2.setPublished(true);
    childContent2.setTags(Set.of());

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of("relatedId1", "relatedId2"));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    // The DTO does not have the searchableContent property
    source.setSearchableContent(null);
    source.setExpandable(false);

    return source;
  }

  private static <T extends ContentDTO> T setOriginalCommonContentDTOProperties(T source) {
    // Set ContentBaseDTO properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    source.setId("id");
    source.setType("type");
    source.setTags(Set.of("tag1", "tag2"));
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set ContentDTO properties
    ContentDTO childContent1 = new ContentDTO();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    ContentDTO childContent2 = new ContentDTO();
    childContent2.setId("child2");
    childContent2.setPublished(true);

    ContentSummaryDTO relatedContent1 = new ContentSummaryDTO();
    relatedContent1.setId("relatedId1");
    ContentSummaryDTO relatedContent2 = new ContentSummaryDTO();
    relatedContent2.setId("relatedId2");

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of(relatedContent1, relatedContent2));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    source.setExpandable(false);

    return source;
  }

  private static <T extends ContentDTO> T setMappedCommonContentDTOProperties(T source) {
    // Set ContentBaseDTO properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    source.setId("id");
    source.setType("type");
    source.setTags(Set.of("tag1", "tag2"));
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set ContentDTO properties
    ContentDTO childContent1 = new ContentDTO();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    ContentDTO childContent2 = new ContentDTO();
    childContent2.setId("child2");
    childContent2.setPublished(true);

    ContentSummaryDTO relatedContent1 = new ContentSummaryDTO();
    relatedContent1.setId("relatedId1");
    ContentSummaryDTO relatedContent2 = new ContentSummaryDTO();
    relatedContent2.setId("relatedId2");

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of(relatedContent1, relatedContent2));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    source.setExpandable(false);

    return source;
  }

  private static AnvilApp prepareAnvilAppDO() {
    AnvilApp object = new AnvilApp();
    object.setAppId("appId");
    object.setAppAccessKey("accessKey");
    return object;
  }

  private static AnvilAppDTO prepareAnvilAppDTO() {
    AnvilAppDTO object = new AnvilAppDTO();
    object.setAppId("appId");
    object.setAppAccessKey("accessKey");
    return object;
  }

  private static Choice prepareOriginalChoiceDO() {
    return prepareOriginalChoiceDO(new Choice());
  }

  private static Choice prepareOriginalChoiceDO(Choice object) {
    ContentBase explanation = new Content();
    explanation.setId("explanationId");

    object.setCorrect(true);
    object.setExplanation(explanation);
    return object;
  }

  private static Choice prepareMappedChoiceDO() {
    // DTO does not have explanation or correct, so these fields will be default
    return new Choice();
  }

  private static ChoiceDTO prepareChoiceDTO() {
    return prepareChoiceDTO(new ChoiceDTO());
  }

  private static ChoiceDTO prepareChoiceDTO(ChoiceDTO object) {
    return object;
  }

  // Choice Questions
  private static ChoiceQuestion prepareOriginalChoiceQuestionDO(ChoiceQuestion object) {
    Choice choice1 = new Choice();
    choice1.setId("choice1");
    Choice choice2 = new Choice();
    choice2.setId("choice2");

    ChoiceQuestion objectWithQuestionFields = (ChoiceQuestion) prepareOriginalQuestionDO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  private static ChoiceQuestion prepareMappedChoiceQuestionDO(ChoiceQuestion object) {
    Choice choice1 = new Choice();
    choice1.setId("choice1");
    choice1.setTags(Set.of());
    Choice choice2 = new Choice();
    choice2.setId("choice2");
    choice2.setTags(Set.of());

    ChoiceQuestion objectWithQuestionFields = (ChoiceQuestion) prepareMappedQuestionDO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  private static ChoiceQuestionDTO prepareOriginalChoiceQuestionDTO(ChoiceQuestionDTO object) {
    ChoiceDTO choice1 = new ChoiceDTO();
    choice1.setId("choice1");
    ChoiceDTO choice2 = new ChoiceDTO();
    choice2.setId("choice2");

    ChoiceQuestionDTO objectWithQuestionFields = (ChoiceQuestionDTO) prepareOriginalQuestionDTO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  private static ChoiceQuestionDTO prepareMappedChoiceQuestionDTO(ChoiceQuestionDTO object) {
    ChoiceDTO choice1 = new ChoiceDTO();
    choice1.setId("choice1");
    ChoiceDTO choice2 = new ChoiceDTO();
    choice2.setId("choice2");

    ChoiceQuestionDTO objectWithQuestionFields = (ChoiceQuestionDTO) prepareMappedQuestionDTO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  // Questions
  private static Question prepareOriginalQuestionDO() {
    return prepareOriginalQuestionDO(new Question());
  }

  private static Question prepareOriginalQuestionDO(Question object) {
    ContentBase answer = new Content();
    answer.setId("answerId");
    Content hint1 = new Content();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    Content hint2 = new Content();
    hint2.setId("hintId2");
    hint2.setPublished(true);
    Content feedback = new Content();
    feedback.setId("feedbackId");

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    object.setDefaultFeedback(feedback);
    return object;
  }

  private static Question prepareMappedQuestionDO() {
    return prepareMappedQuestionDO(new Question());
  }

  private static Question prepareMappedQuestionDO(Question object) {
    ContentBase answer = new Content();
    answer.setId("answerId");
    answer.setTags(Set.of());
    Content hint1 = new Content();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    hint1.setTags(Set.of());
    Content hint2 = new Content();
    hint2.setId("hintId2");
    hint2.setPublished(true);
    hint2.setTags(Set.of());

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    // The DTO does not have the defaultFeedback property
    object.setDefaultFeedback(null);
    return object;
  }

  private static QuestionDTO prepareOriginalQuestionDTO() {
    return prepareOriginalQuestionDTO(new QuestionDTO());
  }

  private static QuestionDTO prepareOriginalQuestionDTO(QuestionDTO object) {
    ContentBaseDTO answer = new ContentDTO();
    answer.setId("answerId");
    ContentDTO hint1 = new ContentDTO();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    ContentDTO hint2 = new ContentDTO();
    hint2.setId("hintId2");
    hint2.setPublished(true);
    QuestionValidationResponseDTO bestAttempt = new QuestionValidationResponseDTO();

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    object.setBestAttempt(bestAttempt);
    return object;
  }

  private static QuestionDTO prepareMappedQuestionDTO() {
    return prepareMappedQuestionDTO(new QuestionDTO());
  }

  private static QuestionDTO prepareMappedQuestionDTO(QuestionDTO object) {
    ContentBaseDTO answer = new ContentDTO();
    answer.setId("answerId");
    ContentDTO hint1 = new ContentDTO();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    ContentDTO hint2 = new ContentDTO();
    hint2.setId("hintId2");
    hint2.setPublished(true);

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    // The DO does not have the bestAttempt property
    object.setBestAttempt(null);
    return object;
  }
}