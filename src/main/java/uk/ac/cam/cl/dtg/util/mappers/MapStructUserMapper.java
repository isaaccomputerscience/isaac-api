package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.GroupMembershipDTO;
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

  // DO <-> DTO Mappings
  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  RegisteredUser map(RegisteredUserDTO source);

  @Mapping(target = "firstLogin", ignore = true)
  RegisteredUserDTO map(RegisteredUser source);

  UserAuthenticationSettings map(UserAuthenticationSettingsDTO source);

  UserAuthenticationSettingsDTO map(UserAuthenticationSettings source);

  AnonymousUser map(AnonymousUserDTO source);

  AnonymousUserDTO map(AnonymousUser source);

  GroupMembership map(GroupMembershipDTO source);

  GroupMembershipDTO map(GroupMembership source);

  @Mapping(target = "status", ignore = true)
  UserGroup map(UserGroupDTO source);

  @Mapping(target = "token", ignore = true)
  @Mapping(target = "ownerSummary", ignore = true)
  @Mapping(target = "mongoId", ignore = true)
  @Mapping(target = "additionalManagers", ignore = true)
  UserGroupDTO map(UserGroup source);

  // Handling classes with multiple mapping targets
  default <T extends UserSummaryDTO> T map(RegisteredUserDTO source, Class<T> targetClass) {
    if (targetClass.equals(UserSummaryForAdminUsersDTO.class)) {
      return (T) mapUserToAdminSummaryDTO(source);
    } else if (targetClass.equals(UserSummaryWithEmailAddressAndGenderDto.class)) {
      return (T) mapUserToSummaryWithEmailAndGenderDTO(source);
    } else if (targetClass.equals(UserSummaryWithEmailAddressDTO.class)) {
      return (T) mapUserToSummaryWithEmailDTO(source);
    } else if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
      return (T) mapUserToSummaryWithGroupMembershipDTO(source);
    } else if (targetClass.equals(UserSummaryDTO.class)) {
      return (T) mapUserToSummary(source);
    } else {
      throw new UnimplementedMappingException(RegisteredUserDTO.class, targetClass);
    }
  }

  default <T> T map(UserFromAuthProvider source, Class<T> targetClass) {
    if (targetClass.equals(RegisteredUser.class)) {
      return (T) mapUserFromAuthProviderToRegisteredUser(source);
    } else {
      throw new UnimplementedMappingException(UserFromAuthProvider.class, targetClass);
    }
  }

  default <T> T map(UserSummaryDTO source, Class<T> targetClass) {
    if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
      return (T) mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(source);
    } else if (targetClass.equals(UserSummaryDTO.class)) {
      return (T) mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(source);
    } else {
      throw new UnimplementedMappingException(UserSummaryDTO.class, targetClass);
    }
  }

  // Mapping an object to a new instance of the same class
  RegisteredUser copy(RegisteredUser source);

  RegisteredUserDTO copy(RegisteredUserDTO source);

  GroupMembership copy(GroupMembership source);

  GroupMembershipDTO copy(GroupMembershipDTO source);

  @Named("copyUserSummaryDTO")
  @SubclassMapping(source = UserSummaryWithEmailAddressAndGenderDto.class,
      target = UserSummaryWithEmailAddressAndGenderDto.class)
  @SubclassMapping(source = UserSummaryWithEmailAddressDTO.class, target = UserSummaryWithEmailAddressDTO.class)
  @SubclassMapping(source = UserSummaryWithGroupMembershipDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
  UserSummaryDTO copy(UserSummaryDTO source);

  UserSummaryWithEmailAddressAndGenderDto copy(UserSummaryWithEmailAddressAndGenderDto source);

  UserSummaryWithEmailAddressDTO copy(UserSummaryWithEmailAddressDTO source);

  UserSummaryWithGroupMembershipDTO copy(UserSummaryWithGroupMembershipDTO source);

  // Mapping to an existing target object, without overwriting any target properties where the source would be null
  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
  void merge(RegisteredUserDTO source, @MappingTarget RegisteredUser target);

  // Specific mappings for use by above mappers
  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryDTO mapUserToSummary(RegisteredUserDTO source);

  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryForAdminUsersDTO mapUserToAdminSummaryDTO(RegisteredUserDTO source);

  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryWithEmailAddressAndGenderDto mapUserToSummaryWithEmailAndGenderDTO(RegisteredUserDTO source);

  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryWithEmailAddressDTO mapUserToSummaryWithEmailDTO(RegisteredUserDTO source);

  @Mapping(target = "groupMembershipInformation", ignore = true)
  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryWithGroupMembershipDTO mapUserToSummaryWithGroupMembershipDTO(RegisteredUserDTO source);

  @Mapping(target = "groupMembershipInformation", ignore = true)
  UserSummaryWithGroupMembershipDTO mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(UserSummaryDTO source);

  @Named("censorUserSummaryDTO")
  @BeanMapping(resultType = UserSummaryDTO.class)
  UserSummaryDTO mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(UserSummaryDTO source);

  @Mapping(target = "teacherPending", ignore = true)
  @Mapping(target = "schoolOther", ignore = true)
  @Mapping(target = "schoolId", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "registrationDate", ignore = true)
  @Mapping(target = "registeredContextsLastConfirmed", ignore = true)
  @Mapping(target = "registeredContexts", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastSeen", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  RegisteredUser mapUserFromAuthProviderToRegisteredUser(UserFromAuthProvider source);
}
