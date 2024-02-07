package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface MainObjectMapper
    extends ContentMapper, UserMapper, EventMapper, MiscMapper, MapStructQuestionValidationMapper {
  MainObjectMapper INSTANCE = Mappers.getMapper(MainObjectMapper.class);
}