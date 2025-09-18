package uk.ac.cam.cl.dtg.isaac.mappers;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

@Mapper
public interface RegisteredUserMapper {
  RegisteredUserMapper INSTANCE = Mappers.getMapper(RegisteredUserMapper.class);

  @Mapping(target = "firstLogin", constant = "false")
  RegisteredUserDTO toDto(RegisteredUser entity);

  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  RegisteredUser toEntity(RegisteredUserDTO dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  @Mapping(target = "registrationDate", ignore = true)
  void updateEntityFromDto(RegisteredUserDTO dto, @MappingTarget RegisteredUser entity);

}

