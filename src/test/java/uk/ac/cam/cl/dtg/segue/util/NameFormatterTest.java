package uk.ac.cam.cl.dtg.segue.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Date;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.NameFormatter;

public class NameFormatterTest {

  /**
   * Test teacher user name extraction.
   */
  @Test
  public final void testGetTeacherNameFromUser() throws Exception {
    // Check case with both first and last name:
    RegisteredUserDTO dummyUserFirstLast = new RegisteredUserDTO();
    dummyUserFirstLast.setGivenName("FirstName");
    dummyUserFirstLast.setFamilyName("LastName");
    String shortNameFirstLast = NameFormatter.getTeacherNameFromUser(dummyUserFirstLast);
    assertEquals("F. LastName", shortNameFirstLast);

    // Check case with no first name:
    RegisteredUserDTO dummyUserNoFirst = new RegisteredUserDTO();
    dummyUserNoFirst.setGivenName("");
    dummyUserNoFirst.setFamilyName("LastName");
    String shortNameNoFirst = NameFormatter.getTeacherNameFromUser(dummyUserNoFirst);
    assertEquals("LastName", shortNameNoFirst);

    UserSummaryDTO dummyUserSummary = new UserSummaryDTO();
    dummyUserSummary.setGivenName("FirstName");
    dummyUserSummary.setFamilyName("LastName");
    String shortNameUserSummary = NameFormatter.getTeacherNameFromUser(dummyUserSummary);
    assertEquals("F. LastName", shortNameUserSummary);
  }

  /**
   * Test teacher user name extraction.
   */
  @Test
  public final void testGetFilteredGroupNameFromGroup() throws Exception {
    String groupName = "Group Name";

    // Check case with shared group name:
    UserGroupDTO dummyGroup = new UserGroupDTO();
    dummyGroup.setLastUpdated(new Date());
    dummyGroup.setGroupName(groupName);
    String filteredGroupName = NameFormatter.getFilteredGroupNameFromGroup(dummyGroup);
    assertEquals(filteredGroupName, groupName);

    // Check case without shared group name:
    UserGroupDTO dummyGroupNotSharedName = new UserGroupDTO();
    dummyGroupNotSharedName.setGroupName(groupName);
    String filteredGroupNameNotShared = NameFormatter.getFilteredGroupNameFromGroup(dummyGroupNotSharedName);
    assertNotEquals("Should not shared group name!", groupName, filteredGroupNameNotShared);
  }
}
