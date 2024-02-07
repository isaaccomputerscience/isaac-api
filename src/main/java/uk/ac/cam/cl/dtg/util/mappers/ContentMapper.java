package uk.ac.cam.cl.dtg.util.mappers;

import java.util.List;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacAnvilQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCard;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCardDeck;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacConceptPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFastTrackQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPageFragment;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuizSection;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacRegexMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacReorderQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicLogicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacTopicSummaryPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.content.AnvilApp;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeTabs;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.EmailTemplate;
import uk.ac.cam.cl.dtg.isaac.dos.content.Figure;
import uk.ac.cam.cl.dtg.isaac.dos.content.Formula;
import uk.ac.cam.cl.dtg.isaac.dos.content.FreeTextRule;
import uk.ac.cam.cl.dtg.isaac.dos.content.GlossaryTerm;
import uk.ac.cam.cl.dtg.isaac.dos.content.GraphChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LogicFormula;
import uk.ac.cam.cl.dtg.isaac.dos.content.Media;
import uk.ac.cam.cl.dtg.isaac.dos.content.Notification;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.content.RegexPattern;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Video;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacAnvilQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDeckDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacConceptPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFastTrackQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfileDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPageFragmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacParsonsQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPodDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacRegexMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacReorderQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicLogicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacTopicSummaryPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.AnvilAppDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeSnippetDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeTabsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FigureDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FormulaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FreeTextRuleDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.GlossaryTermDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.GraphChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.LogicFormulaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.MediaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.NotificationDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ParsonsChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ParsonsItemDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.RegexPatternDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.StringChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.VideoDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.InteractiveCodeSnippet;
import uk.ac.cam.cl.dtg.segue.dto.content.InteractiveCodeSnippetDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, uses = MapStructAudienceContextMapper.class)
public interface ContentMapper {

  ContentMapper INSTANCE = Mappers.getMapper(ContentMapper.class);

  // DO <-> DTO Mappings
  @SubclassMapping(source = ContentDTO.class, target = Content.class)
  ContentBase map(ContentBaseDTO source);

  @SubclassMapping(source = Content.class, target = ContentDTO.class)
  ContentBaseDTO map(ContentBase source);

  @InheritInverseConfiguration(name = "mapContent")
  Content mapContent(ContentDTO source);

  @SubclassMapping(source = AnvilApp.class, target = AnvilAppDTO.class)
  @SubclassMapping(source = Choice.class, target = ChoiceDTO.class)
  @SubclassMapping(source = CodeSnippet.class, target = CodeSnippetDTO.class)
  @SubclassMapping(source = CodeTabs.class, target = CodeTabsDTO.class)
  @SubclassMapping(source = EmailTemplate.class, target = EmailTemplateDTO.class)
  @SubclassMapping(source = GlossaryTerm.class, target = GlossaryTermDTO.class)
  @SubclassMapping(source = IsaacCard.class, target = IsaacCardDTO.class)
  @SubclassMapping(source = IsaacCardDeck.class, target = IsaacCardDeckDTO.class)
  @SubclassMapping(source = IsaacEventPage.class, target = IsaacEventPageDTO.class)
  @SubclassMapping(source = IsaacFeaturedProfile.class, target = IsaacFeaturedProfileDTO.class)
  @SubclassMapping(source = IsaacPageFragment.class, target = IsaacPageFragmentDTO.class)
  @SubclassMapping(source = IsaacPod.class, target = IsaacPodDTO.class)
  @SubclassMapping(source = IsaacQuizSection.class, target = IsaacQuizSectionDTO.class)
  @SubclassMapping(source = IsaacWildcard.class, target = IsaacWildcardDTO.class)
  @SubclassMapping(source = Item.class, target = ItemDTO.class)
  @SubclassMapping(source = Media.class, target = MediaDTO.class)
  @SubclassMapping(source = Notification.class, target = NotificationDTO.class)
  @SubclassMapping(source = Question.class, target = QuestionDTO.class)
  @SubclassMapping(source = SeguePage.class, target = SeguePageDTO.class)
  ContentDTO mapContent(Content source);

  @InheritInverseConfiguration(name = "mapChoice")
  Choice mapChoice(ChoiceDTO source);

  @SubclassMapping(source = Formula.class, target = FormulaDTO.class)
  @SubclassMapping(source = FreeTextRule.class, target = FreeTextRuleDTO.class)
  @SubclassMapping(source = GraphChoice.class, target = GraphChoiceDTO.class)
  @SubclassMapping(source = ParsonsChoice.class, target = ParsonsChoiceDTO.class)
  @SubclassMapping(source = LogicFormula.class, target = LogicFormulaDTO.class)
  @SubclassMapping(source = Quantity.class, target = QuantityDTO.class)
  @SubclassMapping(source = RegexPattern.class, target = RegexPatternDTO.class)
  @SubclassMapping(source = StringChoice.class, target = StringChoiceDTO.class)
  ChoiceDTO mapChoice(Choice source);

  @Mapping(target = "searchableContent", ignore = true)
  @Mapping(target = "explanation", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = ItemChoiceDTO.class, target = ItemChoice.class)
  ItemChoice map(ItemChoiceDTO source);

  @SubclassMapping(source = ItemChoice.class, target = ItemChoiceDTO.class)
  ItemChoiceDTO map(ItemChoice source);

  @Mapping(target = "searchableContent", ignore = true)
  @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = InteractiveCodeSnippet.class)
  CodeSnippet map(CodeSnippetDTO source);

  @SubclassMapping(source = InteractiveCodeSnippet.class, target = InteractiveCodeSnippetDTO.class)
  CodeSnippetDTO map(CodeSnippet source);

  @Mapping(target = "searchableContent", ignore = true)
  @SubclassMapping(source = ImageDTO.class, target = Image.class)
  @SubclassMapping(source = VideoDTO.class, target = Video.class)
  Media map(MediaDTO source);

  @SubclassMapping(source = Image.class, target = ImageDTO.class)
  @SubclassMapping(source = Video.class, target = VideoDTO.class)
  MediaDTO map(Media source);

  @Mapping(target = "searchableContent", ignore = true)
  @SubclassMapping(source = FigureDTO.class, target = Figure.class)
  Image map(ImageDTO source);

  @SubclassMapping(source = Figure.class, target = FigureDTO.class)
  ImageDTO map(Image source);

  @Mapping(target = "searchableContent", ignore = true)
  @SubclassMapping(source = ParsonsItemDTO.class, target = ParsonsItem.class)
  Item map(ItemDTO source);

  @SubclassMapping(source = ParsonsItem.class, target = ParsonsItemDTO.class)
  ItemDTO map(Item source);

  @InheritInverseConfiguration(name = "mapSeguePage")
  SeguePage mapSeguePage(SeguePageDTO source);

  @SubclassMapping(source = IsaacConceptPage.class, target = IsaacConceptPageDTO.class)
  @SubclassMapping(source = IsaacQuestionPage.class, target = IsaacQuestionPageDTO.class)
  @SubclassMapping(source = IsaacQuiz.class, target = IsaacQuizDTO.class)
  @SubclassMapping(source = IsaacTopicSummaryPage.class, target = IsaacTopicSummaryPageDTO.class)
  SeguePageDTO mapSeguePage(SeguePage source);

  @Mapping(target = "searchableContent", ignore = true)
  @SubclassMapping(source = IsaacFastTrackQuestionPageDTO.class, target = IsaacFastTrackQuestionPage.class)
  IsaacQuestionPage map(IsaacQuestionPageDTO source);

  @SubclassMapping(source = IsaacFastTrackQuestionPage.class, target = IsaacFastTrackQuestionPageDTO.class)
  IsaacQuestionPageDTO map(IsaacQuestionPage source);

  @Mapping(target = "searchableContent", ignore = true)
  @Mapping(target = "defaultFeedback", ignore = true)
  @SubclassMapping(source = ChoiceQuestionDTO.class, target = ChoiceQuestion.class)
  Question map(QuestionDTO source);

  @Mapping(target = "bestAttempt", ignore = true)
  @SubclassMapping(source = ChoiceQuestion.class, target = ChoiceQuestionDTO.class)
  QuestionDTO map(Question source);

  @Mapping(target = "searchableContent", ignore = true)
  @Mapping(target = "defaultFeedback", ignore = true)
  @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = IsaacQuestionBase.class)
  ChoiceQuestion map(ChoiceQuestionDTO source);

  @Mapping(target = "bestAttempt", ignore = true)
  @SubclassMapping(source = IsaacQuestionBase.class, target = IsaacQuestionBaseDTO.class)
  ChoiceQuestionDTO map(ChoiceQuestion source);

  @InheritInverseConfiguration(name = "mapIsaacQuestionBase")
  IsaacQuestionBase mapIsaacQuestionBase(IsaacQuestionBaseDTO source);

  @Mapping(target = "bestAttempt", ignore = true)
  @SubclassMapping(source = IsaacAnvilQuestion.class, target = IsaacAnvilQuestionDTO.class)
  @SubclassMapping(source = IsaacFreeTextQuestion.class, target = IsaacFreeTextQuestionDTO.class)
  @SubclassMapping(source = IsaacItemQuestion.class, target = IsaacItemQuestionDTO.class)
  @SubclassMapping(source = IsaacMultiChoiceQuestion.class, target = IsaacMultiChoiceQuestionDTO.class)
  @SubclassMapping(source = IsaacNumericQuestion.class, target = IsaacNumericQuestionDTO.class)
  @SubclassMapping(source = IsaacQuickQuestion.class, target = IsaacQuickQuestionDTO.class)
  @SubclassMapping(source = IsaacRegexMatchQuestion.class, target = IsaacRegexMatchQuestionDTO.class)
  @SubclassMapping(source = IsaacStringMatchQuestion.class, target = IsaacStringMatchQuestionDTO.class)
  @SubclassMapping(source = IsaacSymbolicQuestion.class, target = IsaacSymbolicQuestionDTO.class)
  IsaacQuestionBaseDTO mapIsaacQuestionBase(IsaacQuestionBase source);

  @InheritInverseConfiguration(name = "mapIsaacItemQuestion")
  IsaacItemQuestion mapIsaacItemQuestion(IsaacItemQuestionDTO source);

  @Mapping(target = "bestAttempt", ignore = true)
  @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacClozeQuestionDTO.class)
  @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacParsonsQuestionDTO.class)
  @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacReorderQuestionDTO.class)
  IsaacItemQuestionDTO mapIsaacItemQuestion(IsaacItemQuestion source);

  @Mapping(target = "searchableContent", ignore = true)
  @Mapping(target = "defaultFeedback", ignore = true)
  @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = IsaacSymbolicLogicQuestion.class)
  IsaacSymbolicQuestion mapIsaacSymbolicQuestion(IsaacSymbolicQuestionDTO source);

  @Mapping(target = "bestAttempt", ignore = true)
  @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacSymbolicLogicQuestionDTO.class)
  IsaacSymbolicQuestionDTO mapIsaacSymbolicQuestion(IsaacSymbolicQuestion source);

  // Handling classes with multiple mapping targets
  default <T> T map(ContentDTO source, Class<T> targetClass) {
    if (targetClass.equals(ContentSummaryDTO.class)) {
      return (T) mapContentDTOtoContentSummaryDTO(source);
    } else if (targetClass.equals(QuizSummaryDTO.class)) {
      return (T) mapContentDTOtoQuizSummaryDTO(source);
    } else if (targetClass.equals(IsaacWildcard.class)) {
      return (T) map(mapContent(source), IsaacWildcard.class);
    } else if (targetClass.equals(GameboardItem.class)) {
      return (T) mapContentDTOtoGameboardItem(source);
    } else if (targetClass.equals(Content.class)) {
      return (T) mapContent(source);
    } else {
      throw new UnimplementedMappingException(EventBookingDTO.class, targetClass);
    }
  }

  default <T> T map(Content source, Class<T> targetClass) {
    if (targetClass.equals(IsaacWildcard.class)) {
      return (T) mapContentToIsaacWildcard(source);
    } else if (targetClass.equals(ContentDTO.class)) {
      return (T) mapContent(source);
    } else {
      throw new UnimplementedMappingException(EventBookingDTO.class, targetClass);
    }
  }

  // Mapping an object to a new instance of the same class
  @SubclassMapping(source = ContentDTO.class, target = ContentDTO.class)
  ContentBaseDTO copy(ContentBaseDTO source);

  @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
  ContentDTO copy(ContentDTO source);

  IsaacEventPageDTO copy(IsaacEventPageDTO source);

  Location copy(Location source);

  Address copy(Address source);

  List<Location> copyListOfLocation(List<Location> source);

  List<Address> copyListOfAddress(List<Address> source);

  // Specific mappings for use by above mappers
  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

  @Mapping(target = "visibleToStudents", ignore = true)
  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "hiddenFromRoles", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  QuizSummaryDTO mapContentDTOtoQuizSummaryDTO(ContentDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "description", ignore = true)
  IsaacWildcard mapContentToIsaacWildcard(Content source);

  List<String> mapListOfContentSummaryDtoToListOfString(List<ContentSummaryDTO> source);

  List<ContentSummaryDTO> mapListOfStringToListOfContentSummaryDTO(List<String> source);

  default ContentSummaryDTO mapStringToContentSummaryDTO(String source) {
    if (source == null) {
      return null;
    }

    ContentSummaryDTO contentSummaryDTO = new ContentSummaryDTO();
    contentSummaryDTO.setId(source);
    return contentSummaryDTO;
  }

  default String mapContentSummaryDTOtoString(ContentSummaryDTO source) {
    if (source == null) {
      return null;
    }

    return source.getId();
  }

  List<String> mapListOfGameboardDTOtoListOfString(List<GameboardDTO> source);

  List<GameboardDTO> mapListOfStringToListOfGameboardDTO(List<String> source);

  default GameboardDTO mapStringToGameboardDTO(String source) {
    if (source == null) {
      return null;
    }

    GameboardDTO gameboardDTO = new GameboardDTO();
    gameboardDTO.setId(source);
    return gameboardDTO;
  }

  default String mapGameboardDTOtoString(GameboardDTO source) {
    if (source == null) {
      return null;
    }

    return source.getId();
  }

  @Mapping(target = "uri", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "state", ignore = true)
  @Mapping(target = "questionPartsTotal", ignore = true)
  @Mapping(target = "questionPartsNotAttempted", ignore = true)
  @Mapping(target = "questionPartsIncorrect", ignore = true)
  @Mapping(target = "questionPartsCorrect", ignore = true)
  @Mapping(target = "questionPartStates", ignore = true)
  @Mapping(target = "passMark", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "creationContext", ignore = true)
  @Mapping(target = "contentType", ignore = true)
  @Mapping(target = "boardId", ignore = true)
  @SubclassMapping(source = QuestionDTO.class, target = GameboardItem.class)
  GameboardItem mapContentDTOtoGameboardItem(ContentDTO source);

  @Mapping(target = "uri", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "state", ignore = true)
  @Mapping(target = "questionPartsTotal", ignore = true)
  @Mapping(target = "questionPartsNotAttempted", ignore = true)
  @Mapping(target = "questionPartsIncorrect", ignore = true)
  @Mapping(target = "questionPartsCorrect", ignore = true)
  @Mapping(target = "questionPartStates", ignore = true)
  @Mapping(target = "passMark", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "creationContext", ignore = true)
  @Mapping(target = "contentType", ignore = true)
  @Mapping(target = "boardId", ignore = true)
  @SubclassMapping(source = ChoiceQuestionDTO.class, target = GameboardItem.class)
  GameboardItem mapQuestionDTOtoGameboardItem(QuestionDTO source);

  @Mapping(target = "uri", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "state", ignore = true)
  @Mapping(target = "questionPartsTotal", ignore = true)
  @Mapping(target = "questionPartsNotAttempted", ignore = true)
  @Mapping(target = "questionPartsIncorrect", ignore = true)
  @Mapping(target = "questionPartsCorrect", ignore = true)
  @Mapping(target = "questionPartStates", ignore = true)
  @Mapping(target = "passMark", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "creationContext", ignore = true)
  @Mapping(target = "contentType", ignore = true)
  @Mapping(target = "boardId", ignore = true)
  GameboardItem mapChoiceQuestionDTOtoGameboardItem(ChoiceQuestionDTO source);
}
