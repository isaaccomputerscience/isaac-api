package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserAuthenticationSettingsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryForAdminUsersDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressAndGenderDto;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithGroupMembershipDTO;

@Mapper
public interface MapStructUserMapper {

  MapStructUserMapper INSTANCE = Mappers.getMapper(MapStructUserMapper.class);

  RegisteredUser map(RegisteredUserDTO source);

  RegisteredUserDTO map(RegisteredUser source);

  UserAuthenticationSettings map(UserAuthenticationSettingsDTO source);

  UserAuthenticationSettingsDTO map(UserAuthenticationSettings source);

  AnonymousUser map(AnonymousUserDTO source);

  AnonymousUserDTO map(AnonymousUser source);

  default <T extends UserSummaryDTO> T map(RegisteredUserDTO source, Class<T> targetClass) {
    if (targetClass == UserSummaryForAdminUsersDTO.class) {
      return (T) mapUserToAdminSummaryDTO(source);
    } else if (targetClass == UserSummaryWithEmailAddressAndGenderDto.class) {
      return (T) mapUserToSummaryWithEmailAndGenderDTO(source);
    } else if (targetClass == UserSummaryWithEmailAddressDTO.class) {
      return (T) mapUserToSummaryWithEmailDTO(source);
    } else if (targetClass == UserSummaryWithGroupMembershipDTO.class) {
      return (T) mapUserToSummaryWithGroupMembershipDTO(source);
    } else {
      return (T) mapUserToSummary(source);
    }
  }

  UserSummaryDTO mapUserToSummary(RegisteredUserDTO source);

  UserSummaryForAdminUsersDTO mapUserToAdminSummaryDTO(RegisteredUserDTO source);

  UserSummaryWithEmailAddressAndGenderDto mapUserToSummaryWithEmailAndGenderDTO(RegisteredUserDTO source);

  UserSummaryWithEmailAddressDTO mapUserToSummaryWithEmailDTO(RegisteredUserDTO source);

  UserSummaryWithGroupMembershipDTO mapUserToSummaryWithGroupMembershipDTO(RegisteredUserDTO source);

  RegisteredUserDTO mapToDTO(RegisteredUserDTO source);

  RegisteredUser mapUserToRegisteredUser(UserFromAuthProvider source);
}
