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

package uk.ac.cam.cl.dtg.segue.api.monitors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the user manager class.
 *
 */
class MisuseMonitorTest {
  private PropertiesLoader dummyPropertiesLoader;
  private EmailManager dummyCommunicator;

  /**
   * Initial configuration of tests.
   *
   * @throws Exception
   *             - test exception
   */
  @BeforeEach
  public final void setUp() throws Exception {
    this.dummyCommunicator = createMock(EmailManager.class);
    this.dummyPropertiesLoader = createMock(PropertiesLoader.class);


    expect(dummyPropertiesLoader.getProperty(Constants.SERVER_ADMIN_ADDRESS)).andReturn("FROM ADDRESS").anyTimes();
    replay(this.dummyPropertiesLoader);
  }

  /**
   * Verify that the misusehandler detects misuse.
   */
  @Test
  final void misuseMonitorTokenOwnerLookup_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {
    String userId = "289347298428";
    String event = TokenOwnerLookupMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();
    TokenOwnerLookupMisuseHandler tokenOwnerLookupMisuseHandler =
        new TokenOwnerLookupMisuseHandler(dummyCommunicator, dummyPropertiesLoader);

    misuseMonitor.registerHandler(event, tokenOwnerLookupMisuseHandler);

    dummyCommunicator.addSystemEmailToQueue(EasyMock.isA(EmailCommunicationMessage.class));
    expectLastCall();
    replay(this.dummyCommunicator);

    for (int i = 0; i < tokenOwnerLookupMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown after %d attempts",
              tokenOwnerLookupMisuseHandler.getSoftThreshold()));
    }

    for (int i = tokenOwnerLookupMisuseHandler.getSoftThreshold(); i < tokenOwnerLookupMisuseHandler.getHardThreshold();
         i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception thrown before %d attempts, under limit of %d!", i,
              tokenOwnerLookupMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(userId, event),
        String.format("SegueResourceMisuseException not thrown after %d attempts, over limit of %d!",
            tokenOwnerLookupMisuseHandler.getHardThreshold(),
            tokenOwnerLookupMisuseHandler.getHardThreshold()));

    verify(this.dummyCommunicator, this.dummyPropertiesLoader);
  }

  /**
   * Verifies that the email verification misuse handler is working.
   */
  @Test
  final void emailVerificationRequest_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {

    String event = EmailVerificationRequestMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();

    EmailVerificationRequestMisuseHandler emailVerificationMisuseHandler = new EmailVerificationRequestMisuseHandler();

    misuseMonitor.registerHandler(event, emailVerificationMisuseHandler);

    // Create a test user
    RegisteredUser user = new RegisteredUser();
    user.setEmail("test@test.com");
    user.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);

    // Soft threshold
    for (int i = 0; i < emailVerificationMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(user.getEmail(), event),
          String.format("Exception should not be thrown after %d attempts",
              emailVerificationMisuseHandler.getSoftThreshold()));
    }

    // Hard threshold
    for (int i = emailVerificationMisuseHandler.getSoftThreshold(); i < emailVerificationMisuseHandler.getHardThreshold();
         i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(user.getEmail(), event),
          String.format("Exception thrown before %d attempts, under limit of %d!", i,
              emailVerificationMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(user.getEmail(), event),
        String.format("SegueResourceMisuseException not thrown after %d attempts, over limit of %d!",
            emailVerificationMisuseHandler.getHardThreshold(),
            emailVerificationMisuseHandler.getHardThreshold()));
  }

  /**
   * Verifies that the user search misuse handler is working.
   */
  @Test
  final void userSearchRequest_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {

    String event = UserSearchMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();

    UserSearchMisuseHandler userSearchMisuseHandler = new UserSearchMisuseHandler();

    misuseMonitor.registerHandler(event, userSearchMisuseHandler);

    // Create a test user
    RegisteredUser user = new RegisteredUser();
    user.setId(1234L);
    user.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);

    // Soft threshold
    for (int i = 0; i < userSearchMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(user.getId().toString(), event),
          String.format("Exception should not be thrown after %d attempts",
              userSearchMisuseHandler.getSoftThreshold()));
    }

    // Hard threshold
    for (int i = userSearchMisuseHandler.getSoftThreshold(); i < userSearchMisuseHandler.getHardThreshold();
         i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(user.getId().toString(), event),
          String.format("Exception thrown before %d attempts, under limit of %d!", i,
              userSearchMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(user.getId().toString(), event),
        String.format("SegueResourceMisuseException not thrown after %d attempts, over limit of %d!",
            userSearchMisuseHandler.getHardThreshold(),
            userSearchMisuseHandler.getHardThreshold()));
  }

  /**
   * Verifies that the willHaveMisused method is working.
   */
  @Test
  final void willHaveMisused() {
    String event = UserSearchMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();

    UserSearchMisuseHandler userSearchMisuseHandler = new UserSearchMisuseHandler();

    misuseMonitor.registerHandler(event, userSearchMisuseHandler);

    // Create a test user
    RegisteredUser user = new RegisteredUser();
    user.setId(1234L);
    user.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);

    // Soft threshold
    for (int i = 0; i < userSearchMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(user.getId().toString(), event),
          String.format("Exception should not be thrown after %d attempts",
              userSearchMisuseHandler.getSoftThreshold()));
    }

    // Hard threshold
    for (int i = userSearchMisuseHandler.getSoftThreshold(); i < userSearchMisuseHandler.getHardThreshold();
         i++) {
      assertFalse(misuseMonitor.willHaveMisused(user.getId().toString(), event,
          UserSearchMisuseHandler.HARD_THRESHOLD - i - 1));
      assertTrue(misuseMonitor.willHaveMisused(user.getId().toString(), event,
          UserSearchMisuseHandler.HARD_THRESHOLD - i));
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(user.getId().toString(), event),
          String.format("Exception thrown before %d attempts, under limit of %d!", i,
              userSearchMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(user.getId().toString(), event),
        String.format("SegueResourceMisuseException not thrown after %d attempts, over limit of %d!",
            userSearchMisuseHandler.getHardThreshold(),
            userSearchMisuseHandler.getHardThreshold()));
  }
}
