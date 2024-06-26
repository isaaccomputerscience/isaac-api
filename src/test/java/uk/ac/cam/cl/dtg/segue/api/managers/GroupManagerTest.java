/**
 * Copyright 2014 Stephen Cummins and Nick Rogers
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.easymock.Capture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.UserMapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the user manager class.
 */
class GroupManagerTest {
  private UserMapper dummyMapper;

  private IUserGroupPersistenceManager groupDataManager;
  private UserAccountManager userManager;
  private GameManager gameManager;

  private GroupManager groupManager;

  private static final Instant somePastDate = Instant.now().minus(7L, ChronoUnit.DAYS);

  /**
   * Initial configuration of tests.
   *
   * @throws Exception - test exception
   */
  @BeforeEach
  public final void setUp() throws Exception {
    this.dummyMapper = createMock(UserMapper.class);
    this.groupDataManager = createMock(IUserGroupPersistenceManager.class);
    this.userManager = createMock(UserAccountManager.class);
    this.gameManager = createMock(GameManager.class);

    this.groupManager = new GroupManager(this.groupDataManager, this.userManager, this.gameManager, this.dummyMapper);

    PropertiesLoader dummyPropertiesLoader = createMock(PropertiesLoader.class);
    expect(dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_DEFAULT)).andReturn("60").anyTimes();
    replay(dummyPropertiesLoader);
  }

  /**
   * Verify that the constructor responds correctly to bad input.
   */
  @Test
  final void groupManager_createValidGroup_aGroupShouldBeCreated() {
    String someGroupName = "Group Name";
    RegisteredUserDTO someGroupOwner = new RegisteredUserDTO();
    Long someGroupOwnerId = 5339L;
    someGroupOwner.setId(someGroupOwnerId);
    someGroupOwner.setEmail("test@test.com");
    Set<Long> someSetOfManagers = Sets.newHashSet();
    Capture<UserGroup> capturedGroup = Capture.newInstance();

    List<RegisteredUserDTO> someListOfUsers = Lists.newArrayList();
    List<UserSummaryWithEmailAddressDTO> someListOfUsersDTOs = Lists.newArrayList();

    UserSummaryWithEmailAddressDTO someGroupOwnerSummary = new UserSummaryWithEmailAddressDTO();
    someGroupOwnerSummary.setId(someGroupOwnerId);
    someGroupOwnerSummary.setEmail("test@test.com");
    UserGroup resultFromDB = new UserGroup();
    resultFromDB.setId(2L);
    resultFromDB.setGroupName(someGroupName);
    resultFromDB.setOwnerId(someGroupOwnerId);
    UserGroupDTO mappedGroup = new UserGroupDTO();
    mappedGroup.setId(2L);
    mappedGroup.setGroupName(someGroupName);
    mappedGroup.setOwnerId(someGroupOwnerId);

    try {
      expect(this.groupDataManager.createGroup(and(capture(capturedGroup), isA(UserGroup.class))))
          .andReturn(resultFromDB);
      expect(this.groupDataManager.getAdditionalManagerSetByGroupId(anyObject()))
          .andReturn(someSetOfManagers).atLeastOnce();
      expect(this.userManager.findUsers(someSetOfManagers)).andReturn(someListOfUsers);
      expect(this.userManager.getUserDTOById(null)).andThrow(new NoUserException("No user found with this ID!"));
      expect(this.userManager.convertToDetailedUserSummaryObjectList(someListOfUsers,
          UserSummaryWithEmailAddressDTO.class)).andReturn(someListOfUsersDTOs);
      expect(this.dummyMapper.map(resultFromDB)).andReturn(mappedGroup).atLeastOnce();
      expect(this.userManager.getUserDTOById(someGroupOwnerId)).andReturn(someGroupOwner);
      expect(this.userManager.convertToUserSummary(someGroupOwner, UserSummaryWithEmailAddressDTO.class)).andReturn(
          someGroupOwnerSummary);

      replay(this.userManager, this.groupDataManager, this.dummyMapper);

      // check that the result of the method is whatever comes out of the database
      UserGroupDTO createUserGroup = this.groupManager.createUserGroup(someGroupName, someGroupOwner);
      assertEquals(mappedGroup, createUserGroup);

      // check that what goes into the database is what we passed it.
      assertEquals(someGroupOwner.getId(), capturedGroup.getValue().getOwnerId());
      assertEquals(someGroupName, capturedGroup.getValue().getGroupName());
      assertInstanceOf(Instant.class, capturedGroup.getValue().getCreated());

    } catch (SegueDatabaseException | NoUserException e) {
      fail("No exception expected", e);
    }
    verify(this.groupDataManager);
  }

  @Test
  void orderUsersByName_ordersBySurnamePrimarily() {
    List<RegisteredUserDTO> users = Stream.of(
            new RegisteredUserDTO("A", "Ab", "aab@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE,
                somePastDate, "", false),
            new RegisteredUserDTO("B", "Ar", "bar@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE,
                somePastDate, "", false),
            new RegisteredUserDTO("C", "Ax", "caz@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE,
                somePastDate, "", false),
            new RegisteredUserDTO(null, "Ax", "NONEax@test.com", EmailVerificationStatus.VERIFIED, somePastDate,
                Gender.FEMALE, somePastDate, "", false),
            new RegisteredUserDTO("A", "Ba", "dba@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE,
                somePastDate, "", false),
            new RegisteredUserDTO("B", "Bb", "ebb@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE,
                somePastDate, "", false),
            new RegisteredUserDTO("C", "Bf", "fbf@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE,
                somePastDate, "", false),
            new RegisteredUserDTO("A", null, "aNONE@test.com", EmailVerificationStatus.VERIFIED, somePastDate,
                Gender.FEMALE, somePastDate, "", false))
        .peek(user -> user.setId((long) (user.getGivenName() + user.getFamilyName()).hashCode()))
        .collect(Collectors.toList());

    List<RegisteredUserDTO> shuffledUsers = new ArrayList<>(users);
    Collections.shuffle(shuffledUsers);

    List<RegisteredUserDTO> sortedUsers = this.groupManager.orderUsersByName(shuffledUsers);
    assertEquals(users, sortedUsers);
  }
}
