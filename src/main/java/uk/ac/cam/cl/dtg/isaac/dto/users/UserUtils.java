package uk.ac.cam.cl.dtg.isaac.dto.users;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;

public final class UserUtils {
  private UserUtils() {}

  public static String trimIfNotNull(String value) {
    return value != null ? value.trim() : null;
  }

  public static boolean compareCommonFields(RegisteredUser user, RegisteredUserDTO dto) {
    if (user == null && dto == null) return true;
    if (user == null || dto == null) return false;

    return Objects.equals(user.getId(), dto.getId()) &&
        Objects.equals(user.getDateOfBirth(), dto.getDateOfBirth()) &&
        Objects.equals(user.getEmail(), dto.getEmail()) &&
        Objects.equals(user.getEmailVerificationStatus(), dto.getEmailVerificationStatus()) &&
        Objects.equals(user.getFamilyName(), dto.getFamilyName()) &&
        user.getGender() == dto.getGender() &&
        Objects.equals(user.getGivenName(), dto.getGivenName()) &&
        Objects.equals(user.getLastUpdated(), dto.getLastUpdated()) &&
        Objects.equals(user.getPrivacyPolicyAcceptedTime(), dto.getPrivacyPolicyAcceptedTime()) &&
        Objects.equals(user.getRegistrationDate(), dto.getRegistrationDate()) &&
        user.getRole() == dto.getRole() &&
        Objects.equals(user.getSchoolId(), dto.getSchoolId()) &&
        Objects.equals(user.getSchoolOther(), dto.getSchoolOther()) &&
        Objects.equals(user.getTeacherPending(), dto.getTeacherPending()) &&
        Objects.equals(user.getLastSeen(), dto.getLastSeen()) &&
        Objects.equals(user.getRegisteredContexts(), dto.getRegisteredContexts()) &&
        Objects.equals(user.getRegisteredContextsLastConfirmed(), dto.getRegisteredContextsLastConfirmed());
  }

  public static String buildCommonToString(String className, Long id, String givenName,
                                           String familyName, String email, Role role,
                                           Instant dateOfBirth, Gender gender,
                                           Instant registrationDate, String schoolId,
                                           String schoolOther, EmailVerificationStatus emailVerificationStatus,
                                           Boolean teacherPending, Instant lastUpdated,
                                           Instant privacyPolicyAcceptedTime, Instant lastSeen,
                                           List<UserContext> registeredContexts,
                                           Instant registeredContextsLastConfirmed) {
    return String.format(
        "%s{id=%d, givenName='%s', familyName='%s', email='%s', role=%s, " +
            "dateOfBirth=%s, gender=%s, registrationDate=%s, schoolId='%s', schoolOther='%s', " +
            "emailVerificationStatus=%s, teacherPending=%s, lastUpdated=%s, " +
            "privacyPolicyAcceptedTime=%s, lastSeen=%s, registeredContexts=%s, " +
            "registeredContextsLastConfirmed=%s",
        className, id, givenName, familyName, email, role, dateOfBirth, gender,
        registrationDate, schoolId, schoolOther, emailVerificationStatus, teacherPending,
        lastUpdated, privacyPolicyAcceptedTime, lastSeen, registeredContexts,
        registeredContextsLastConfirmed
    );
  }
}
