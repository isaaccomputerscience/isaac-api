package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface MapStructMainMapper extends MapStructContentMapper, MapStructUserMapper {
  MapStructMainMapper INSTANCE = Mappers.getMapper(MapStructMainMapper.class);
}
