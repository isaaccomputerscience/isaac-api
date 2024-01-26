package uk.ac.cam.cl.dtg.util;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserAuthenticationSettingsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryForAdminUsersDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressAndGenderDto;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithGroupMembershipDTO;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface MapStructContentMapper {
  MapStructContentMapper INSTANCE = Mappers.getMapper(MapStructContentMapper.class);

  @SubclassMapping(source = ContentDTO.class, target = Content.class)
  ContentBase map(ContentBaseDTO source);

  @SubclassMapping(source = Content.class, target = ContentDTO.class)
  ContentBaseDTO map(ContentBase source);

  Content map(ContentDTO source);

  ContentDTO map(Content source);

  @Mapping(target = "id", source = "source")
  ContentSummaryDTO map(String source);

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

  List<String> mapContentSummaryDtoToString(List<ContentSummaryDTO> source);

  List<ContentSummaryDTO> mapStringToContentSummaryDTO(List<String> source);

  default String mapToString(ContentSummaryDTO source) {
    return source.getId();
  }
}
