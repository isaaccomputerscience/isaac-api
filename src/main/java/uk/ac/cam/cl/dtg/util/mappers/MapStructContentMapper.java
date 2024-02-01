package uk.ac.cam.cl.dtg.util.mappers;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.Formula;
import uk.ac.cam.cl.dtg.isaac.dos.content.FreeTextRule;
import uk.ac.cam.cl.dtg.isaac.dos.content.GraphChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LogicFormula;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.isaac.dos.content.RegexPattern;
import uk.ac.cam.cl.dtg.isaac.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FormulaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FreeTextRuleDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.GraphChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.LogicFormulaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ParsonsChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.RegexPatternDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.StringChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface MapStructContentMapper {

  MapStructContentMapper INSTANCE = Mappers.getMapper(MapStructContentMapper.class);

  // DO <-> DTO Mappings
  @SubclassMapping(source = ContentDTO.class, target = Content.class)
  ContentBase map(ContentBaseDTO source);

  @SubclassMapping(source = Content.class, target = ContentDTO.class)
  ContentBaseDTO map(ContentBase source);

  @SubclassMapping(source = ChoiceDTO.class, target = Choice.class)
  Content map(ContentDTO source);

  @SubclassMapping(source = Choice.class, target = ChoiceDTO.class)
  ContentDTO map(Content source);

  @SubclassMapping(source = FormulaDTO.class, target = Formula.class)
  @SubclassMapping(source = FreeTextRuleDTO.class, target = FreeTextRule.class)
  @SubclassMapping(source = GraphChoiceDTO.class, target = GraphChoice.class)
  @SubclassMapping(source = ParsonsChoiceDTO.class, target = ParsonsChoice.class)
  @SubclassMapping(source = ItemChoiceDTO.class, target = ItemChoice.class)
  @SubclassMapping(source = LogicFormulaDTO.class, target = LogicFormula.class)
  @SubclassMapping(source = QuantityDTO.class, target = Quantity.class)
  @SubclassMapping(source = RegexPatternDTO.class, target = RegexPattern.class)
  @SubclassMapping(source = StringChoiceDTO.class, target = StringChoice.class)
  Choice map(ChoiceDTO source);

  // Note: In theory, @InheritInverseConfiguration could help here, but it was having ambiguity problems
  @SubclassMapping(target = FormulaDTO.class, source = Formula.class)
  @SubclassMapping(target = FreeTextRuleDTO.class, source = FreeTextRule.class)
  @SubclassMapping(target = GraphChoiceDTO.class, source = GraphChoice.class)
  @SubclassMapping(target = ParsonsChoiceDTO.class, source = ParsonsChoice.class)
  @SubclassMapping(target = ItemChoiceDTO.class, source = ItemChoice.class)
  @SubclassMapping(target = LogicFormulaDTO.class, source = LogicFormula.class)
  @SubclassMapping(target = QuantityDTO.class, source = Quantity.class)
  @SubclassMapping(target = RegexPatternDTO.class, source = RegexPattern.class)
  @SubclassMapping(target = StringChoiceDTO.class, source = StringChoice.class)
  ChoiceDTO map(Choice source);

  // Handling classes with multiple mapping targets
  default <T> T map(ContentDTO source, Class<T> targetClass) {
    if (targetClass.equals(ContentSummaryDTO.class)) {
      return (T) mapContentDTOtoContentSummaryDTO(source);
    } else if (targetClass.equals(QuizSummaryDTO.class)) {
      return (T) mapContentDTOtoQuizSummaryDTO(source);
    } else if (targetClass.equals(IsaacWildcard.class)) {
      return (T) map(map(source), IsaacWildcard.class);
    } else if (targetClass.equals(GameboardItem.class)) {
      return (T) mapContentDTOtoGameboardItem(source);
    } else if (targetClass.equals(Content.class)) {
      return (T) map(source);
    } else {
      throw new UnimplementedMappingException(EventBookingDTO.class, targetClass);
    }
  }

  default <T> T map(Content source, Class<T> targetClass) {
    if (targetClass.equals(IsaacWildcard.class)) {
      return (T) mapContentToIsaacWildcard(source);
    } else if (targetClass.equals(ContentDTO.class)) {
      return (T) map(source);
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

  // Specific mappings for use by above mappers
  ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

  QuizSummaryDTO mapContentDTOtoQuizSummaryDTO(ContentDTO source);

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

  @SubclassMapping(source = QuestionDTO.class, target = GameboardItem.class)
  GameboardItem mapContentDTOtoGameboardItem(ContentDTO source);

  @SubclassMapping(source = ChoiceQuestionDTO.class, target = GameboardItem.class)
  GameboardItem mapQuestionDTOtoGameboardItem(QuestionDTO source);

  GameboardItem mapChoiceQuestionDTOtoGameboardItem(ChoiceQuestionDTO source);
}
