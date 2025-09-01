package uk.ac.cam.cl.dtg.isaac.service;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.InvalidTimestampException;
import uk.ac.cam.cl.dtg.isaac.api.requests.PrivacyPolicyRequest;
import uk.ac.cam.cl.dtg.isaac.api.services.PrivacyPolicyService;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

@ExtendWith(EasyMockExtension.class)
public class PrivacyPolicyServiceTest {

  @Mock
  private UserAccountManager userManager;

  @Mock
  private Clock mockClock;

  @Mock
  private HttpServletRequest request;

  private PrivacyPolicyService service;
  private PrivacyPolicyRequest privacyPolicyRequest;
  private RegisteredUserDTO testUser;
  private final Instant baseTime = Instant.parse("2023-08-29T12:00:00Z");

  @BeforeEach
  public void setUp() {
    service = new PrivacyPolicyService(userManager, mockClock);

    privacyPolicyRequest = new PrivacyPolicyRequest();
    privacyPolicyRequest.setPrivacyPolicyAcceptedTime(baseTime.toEpochMilli());

    testUser = new RegisteredUserDTO();
    testUser.setEmail("test@example.com");

    expect(mockClock.instant()).andReturn(baseTime).anyTimes();
    replay(mockClock);
  }

  @Test
  public void testAcceptPrivacyPolicy_Success() throws Exception {
    expect(userManager.getCurrentRegisteredUser(request)).andReturn(testUser);
    userManager.updatePrivacyPolicyAcceptedTime(testUser, baseTime);
    expectLastCall().once();

    replay(userManager);

    service.acceptPrivacyPolicy(request, privacyPolicyRequest);

    verify(userManager);
  }

  @Test
  public void testAcceptPrivacyPolicy_TimestampWithinTolerance() throws Exception {
    // Timestamp 20 seconds in the future (within 30s tolerance)
    Instant futureTime = baseTime.plusSeconds(20);
    privacyPolicyRequest.setPrivacyPolicyAcceptedTime(futureTime.toEpochMilli());

    expect(userManager.getCurrentRegisteredUser(request)).andReturn(testUser);
    userManager.updatePrivacyPolicyAcceptedTime(testUser, futureTime);
    expectLastCall().once();

    replay(userManager);

    service.acceptPrivacyPolicy(request, privacyPolicyRequest);
    verify(userManager);
  }

  @Test
  public void testAcceptPrivacyPolicy_TimestampTooFarInFuture() throws Exception {
    // Timestamp 60 seconds in the future (outside 30s tolerance)
    Instant farFutureTime = baseTime.plusSeconds(60);
    privacyPolicyRequest.setPrivacyPolicyAcceptedTime(farFutureTime.toEpochMilli());

    expect(userManager.getCurrentRegisteredUser(request)).andReturn(testUser);
    replay(userManager);

    InvalidTimestampException exception = assertThrows(InvalidTimestampException.class, () -> {
      service.acceptPrivacyPolicy(request, privacyPolicyRequest);
    });

    assertTrue(exception.getMessage().contains("Timestamp too far from current time"));
    verify(userManager);
  }

  @Test
  public void testAcceptPrivacyPolicy_TimestampTooFarInPast() throws Exception {
    Instant pastTime = baseTime.minusSeconds(45);
    privacyPolicyRequest.setPrivacyPolicyAcceptedTime(pastTime.toEpochMilli());

    expect(userManager.getCurrentRegisteredUser(request)).andReturn(testUser);
    replay(userManager);

    InvalidTimestampException exception = assertThrows(InvalidTimestampException.class, () -> {
      service.acceptPrivacyPolicy(request, privacyPolicyRequest);
    });

    assertTrue(exception.getMessage().contains("Timestamp too far from current time"));
    verify(userManager);
  }

  @Test
  public void testAcceptPrivacyPolicy_NoUserLoggedIn() throws Exception {
    expect(userManager.getCurrentRegisteredUser(request))
        .andThrow(new NoUserLoggedInException());

    replay(userManager);

    assertThrows(NoUserLoggedInException.class, () -> {
      service.acceptPrivacyPolicy(request, privacyPolicyRequest);
    });

    verify(userManager);
  }

  @Test
  public void testAcceptPrivacyPolicy_DatabaseError() throws Exception {
    expect(userManager.getCurrentRegisteredUser(request)).andReturn(testUser);
    userManager.updatePrivacyPolicyAcceptedTime(testUser, baseTime);
    expectLastCall().andThrow(new SegueDatabaseException("Database connection failed"));

    replay(userManager);

    assertThrows(SegueDatabaseException.class, () -> {
      service.acceptPrivacyPolicy(request, privacyPolicyRequest);
    });

    verify(userManager);
  }
}
