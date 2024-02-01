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

  GameboardDTO map(GameboardDO source);

  GameboardDO map(GameboardDTO source);

  AssignmentDTO map(AssignmentDO source);

  AssignmentDO map(AssignmentDTO source);

  QuizAssignmentDTO map(QuizAssignmentDO source);

  QuizAssignmentDO map(QuizAssignmentDTO source);

  QuizAttemptDTO map(QuizAttemptDO source);

  QuizAttemptDO map(QuizAttemptDTO source);

  default ResultsWrapper<String> copy(ResultsWrapper<String> source) {
    return new ResultsWrapper<>(source.getResults(), source.getTotalResults());
  }
}
