package uk.ac.cam.cl.dtg.util.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.elasticsearch.core.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.RoleRequirement;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.content.AnvilApp;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.EmailTemplate;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.AnvilAppDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

class ContentMapperTest {
  private final ContentMapper mapper = ContentMapper.INSTANCE;

  private final ObjectMapper jsonMapper = new ObjectMapper();

  private static final Date testDate = new Date();

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
        Arguments.of(prepareEmailTemplateDO(), prepareEmailTemplateDTO()),
        Arguments.of(prepareImageDO(), prepareImageDTO()),
        Arguments.of(prepareIsaacEventPageDO(), prepareMappedIsaacEventPageDTO()),
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
        Arguments.of(prepareEmailTemplateDTO(), prepareEmailTemplateDO()),
        Arguments.of(prepareImageDTO(), prepareImageDO()),
        Arguments.of(prepareOriginalIsaacEventPageDTO(), prepareIsaacEventPageDO()),
        Arguments.of(prepareOriginalQuestionDTO(), prepareMappedQuestionDO())
    );
  }

  // Common properties for Content objects, including those inherited from the abstract ContentBase class
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

  // Choice
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

  // Choice Question
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

  // EmailTemplate
  private static EmailTemplate prepareEmailTemplateDO() {
    EmailTemplate object = new EmailTemplate();
    object.setSubject("subject");
    object.setPlainTextContent("textContent");
    object.setHtmlContent("htmlContent");
    object.setOverrideFromAddress("fromAddress");
    object.setOverrideFromName("fromName");
    object.setOverrideEnvelopeFrom("envelopeFrom");
    object.setReplyToEmailAddress("replyEmail");
    object.setReplyToName("replyName");
    return object;
  }

  private static EmailTemplateDTO prepareEmailTemplateDTO() {
    EmailTemplateDTO object = new EmailTemplateDTO();
    object.setSubject("subject");
    object.setPlainTextContent("textContent");
    object.setHtmlContent("htmlContent");
    object.setOverrideFromAddress("fromAddress");
    object.setOverrideFromName("fromName");
    object.setOverrideEnvelopeFrom("envelopeFrom");
    object.setReplyToEmailAddress("replyEmail");
    object.setReplyToName("replyName");
    return object;
  }

  // Image
  private static Image prepareImageDO() {
    Image object = new Image();
    object.setSrc("imageSource");
    object.setAltText("altText");
    object.setClickUrl("clickUrl");
    object.setClickTarget("clickTarget");
    return object;
  }

  private static ImageDTO prepareImageDTO() {
    ImageDTO object = new ImageDTO();
    object.setSrc("imageSource");
    object.setAltText("altText");
    object.setClickUrl("clickUrl");
    object.setClickTarget("clickTarget");
    return object;
  }

  // IsaacEventPage
  private static IsaacEventPage prepareIsaacEventPageDO() {
    Address address = new Address();
    Location location = new Location(address, 3.0, 7.0);
    ExternalReference preResource1 = new ExternalReference();
    preResource1.setTitle("title1");
    preResource1.setUrl("url1");
    ExternalReference preResource2 = new ExternalReference();
    preResource2.setTitle("title2");
    preResource2.setUrl("url2");
    Content preResourceContent1 = new Content();
    preResourceContent1.setId("preResourceContent1");
    preResourceContent1.setTags(Set.of());
    Content preResourceContent2 = new Content();
    preResourceContent2.setId("preResourceContent2");
    preResourceContent2.setTags(Set.of());
    ExternalReference postResource1 = new ExternalReference();
    postResource1.setTitle("title1");
    postResource1.setUrl("url1");
    ExternalReference postResource2 = new ExternalReference();
    postResource2.setTitle("title2");
    postResource2.setUrl("url2");
    Content postResourceContent1 = new Content();
    postResourceContent1.setId("postResourceContent1");
    postResourceContent1.setTags(Set.of());
    Content postResourceContent2 = new Content();
    postResourceContent2.setId("postResourceContent2");
    postResourceContent2.setTags(Set.of());
    Image eventThumbnail = new Image();
    eventThumbnail.setSrc("thumbnailSource");
    eventThumbnail.setPublished(true);
    eventThumbnail.setTags(Set.of());

    IsaacEventPage object = new IsaacEventPage();
    object.setDate(testDate);
    object.setEndDate(testDate);
    object.setBookingDeadline(testDate);
    object.setPrepWorkDeadline(testDate);
    object.setLocation(location);
    object.setPreResources(List.of(preResource1, preResource2));
    object.setPreResourceContent(List.of(preResourceContent1, preResourceContent2));
    object.setEmailEventDetails("emailEventDetails");
    object.setEmailConfirmedBookingText("confirmedBookingText");
    object.setEmailWaitingListBookingText("waitingListText");
    object.setPostResources(List.of(postResource1, postResource2));
    object.setPostResourceContent(List.of(postResourceContent1, postResourceContent2));
    object.setEventThumbnail(eventThumbnail);
    object.setNumberOfPlaces(100);
    object.setEventStatus(EventStatus.OPEN);
    object.setIsaacGroupToken("groupToken");
    object.setGroupReservationLimit(20);
    object.setAllowGroupReservations(true);
    object.setPrivateEvent(true);
    return object;
  }

  private static IsaacEventPageDTO prepareOriginalIsaacEventPageDTO() {
    IsaacEventPageDTO object = prepareIsaacEventPageDTO(new IsaacEventPageDTO());
    object.setUserBookingStatus(BookingStatus.CONFIRMED);
    return object;
  }

  private static IsaacEventPageDTO prepareMappedIsaacEventPageDTO() {
    IsaacEventPageDTO object = prepareIsaacEventPageDTO(new IsaacEventPageDTO());
    object.setUserBookingStatus(null);
    return object;
  }

  private static IsaacEventPageDTO prepareIsaacEventPageDTO(IsaacEventPageDTO object) {
    Address address = new Address();
    Location location = new Location(address, 3.0, 7.0);
    ExternalReference preResource1 = new ExternalReference();
    preResource1.setTitle("title1");
    preResource1.setUrl("url1");
    ExternalReference preResource2 = new ExternalReference();
    preResource2.setTitle("title2");
    preResource2.setUrl("url2");
    ContentDTO preResourceContent1 = new ContentDTO();
    preResourceContent1.setId("preResourceContent1");
    ContentDTO preResourceContent2 = new ContentDTO();
    preResourceContent2.setId("preResourceContent2");
    ExternalReference postResource1 = new ExternalReference();
    postResource1.setTitle("title1");
    postResource1.setUrl("url1");
    ExternalReference postResource2 = new ExternalReference();
    postResource2.setTitle("title2");
    postResource2.setUrl("url2");
    ContentDTO postResourceContent1 = new ContentDTO();
    postResourceContent1.setId("postResourceContent1");
    ContentDTO postResourceContent2 = new ContentDTO();
    postResourceContent2.setId("postResourceContent2");
    ImageDTO eventThumbnail = new ImageDTO();
    eventThumbnail.setSrc("thumbnailSource");
    eventThumbnail.setPublished(true);

    object.setDate(testDate);
    object.setEndDate(testDate);
    object.setBookingDeadline(testDate);
    object.setPrepWorkDeadline(testDate);
    object.setLocation(location);
    object.setPreResources(List.of(preResource1, preResource2));
    object.setPreResourceContent(List.of(preResourceContent1, preResourceContent2));
    object.setEmailEventDetails("emailEventDetails");
    object.setEmailConfirmedBookingText("confirmedBookingText");
    object.setEmailWaitingListBookingText("waitingListText");
    object.setPostResources(List.of(postResource1, postResource2));
    object.setPostResourceContent(List.of(postResourceContent1, postResourceContent2));
    object.setEventThumbnail(eventThumbnail);
    object.setNumberOfPlaces(100);
    object.setEventStatus(EventStatus.OPEN);
    object.setIsaacGroupToken("groupToken");
    object.setGroupReservationLimit(20);
    object.setAllowGroupReservations(true);
    object.setPrivateEvent(true);
    return object;
  }

  // Question
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