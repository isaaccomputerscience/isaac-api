package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;

@Mapper
public interface MapStructMiscMapper {

  MapStructMiscMapper INSTANCE = Mappers.getMapper(MapStructMiscMapper.class);

  GameboardDTO map(GameboardDO source);

  GameboardDO map(GameboardDTO source);

  default ResultsWrapper<String> copy(ResultsWrapper<String> source) {
    return new ResultsWrapper<>(source.getResults(), source.getTotalResults());
  }
}
