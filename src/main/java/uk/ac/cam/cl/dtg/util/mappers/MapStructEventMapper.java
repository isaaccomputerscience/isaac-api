package uk.ac.cam.cl.dtg.util.mappers;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;

@Mapper
public interface MapStructEventMapper {

  MapStructEventMapper INSTANCE = Mappers.getMapper(MapStructEventMapper.class);

  // Handling classes with multiple mapping targets
  default <T extends EventBookingDTO> T map(DetailedEventBookingDTO source, Class<T> targetClass) {
    if (targetClass.equals(EventBookingDTO.class)) {
      return (T) mapDetailedEventBookingDTOtoEventBookingDTO(source);
    } else {
      throw new UnimplementedMappingException(EventBookingDTO.class, targetClass);
    }
  }

  default <S extends EventBookingDTO, T extends EventBookingDTO> List<T> mapList(List<S> source, Class<S> sourceClass,
                                                                                 Class<T> targetClass) {
    if (sourceClass.equals(EventBookingDTO.class) && targetClass.equals(EventBookingDTO.class)) {
      return (List<T>) copyListOfEventBookingDTO((List<EventBookingDTO>) source);
    } else if (sourceClass.equals(DetailedEventBookingDTO.class) && targetClass.equals(EventBookingDTO.class)) {
      return (List<T>) mapListOfDetailedEventBookingDTOtoEventBookingDTO((List<DetailedEventBookingDTO>) source);
    } else {
      throw new UnimplementedMappingException(EventBookingDTO.class, targetClass);
    }
  }

  // Mapping an object to a new instance of the same class
  EventBookingDTO copy(EventBookingDTO source);

  // Specific mappings for use by above mappers
  List<EventBookingDTO> copyListOfEventBookingDTO(List<EventBookingDTO> source);

  EventBookingDTO mapDetailedEventBookingDTOtoEventBookingDTO(DetailedEventBookingDTO source);

  List<EventBookingDTO> mapListOfDetailedEventBookingDTOtoEventBookingDTO(List<DetailedEventBookingDTO> source);

}
