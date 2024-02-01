package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAttemptDO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;

@Mapper
public interface MapStructMiscMapper {

  MapStructMiscMapper INSTANCE = Mappers.getMapper(MapStructMiscMapper.class);

  // DO <-> DTO Mappings
  GameboardDO map(GameboardDTO source);

  GameboardDTO map(GameboardDO source);

  AssignmentDO map(AssignmentDTO source);

  AssignmentDTO map(AssignmentDO source);

  QuizAssignmentDO map(QuizAssignmentDTO source);

  QuizAssignmentDTO map(QuizAssignmentDO source);

  QuizAttemptDO map(QuizAttemptDTO source);

  QuizAttemptDTO map(QuizAttemptDO source);

  // Mapping an object to a new instance of the same class
  default ResultsWrapper<String> copy(ResultsWrapper<String> source) {
    return new ResultsWrapper<>(source.getResults(), source.getTotalResults());
  }
}
