package uk.ac.cam.cl.dtg.util.mappers;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface MapStructContentMapper {

  MapStructContentMapper INSTANCE = Mappers.getMapper(MapStructContentMapper.class);

  @SubclassMapping(source = ContentDTO.class, target = Content.class)
  ContentBase map(ContentBaseDTO source);

  @SubclassMapping(source = Content.class, target = ContentDTO.class)
  ContentBaseDTO map(ContentBase source);

  Content map(ContentDTO source);

  ContentDTO map(Content source);

  default <T> T map(ContentDTO source, Class<T> targetClass) {
    if (targetClass.equals(ContentSummaryDTO.class)) {
      return (T) mapContentDTOtoContentSummaryDTO(source);
    } else {
      throw new UnimplementedMappingException(EventBookingDTO.class, targetClass);
    }
  }

  @Mapping(target = "id", source = "source")
  ContentSummaryDTO map(String source);

  ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

  List<String> mapContentSummaryDtoToString(List<ContentSummaryDTO> source);

  List<ContentSummaryDTO> mapStringToContentSummaryDTO(List<String> source);

  default String mapToString(ContentSummaryDTO source) {
    return source.getId();
  }

  @SubclassMapping(source = ContentDTO.class, target = ContentDTO.class)
  ContentBaseDTO copy(ContentBaseDTO source);

  @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
  ContentDTO copy(ContentDTO source);

  IsaacEventPageDTO copy(IsaacEventPageDTO source);
}
