package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;

@Mapper
public interface MapStructGameboardMapper {

  MapStructGameboardMapper INSTANCE = Mappers.getMapper(MapStructGameboardMapper.class);

  GameboardDTO map(GameboardDO source);

  GameboardDO map(GameboardDTO source);
}
