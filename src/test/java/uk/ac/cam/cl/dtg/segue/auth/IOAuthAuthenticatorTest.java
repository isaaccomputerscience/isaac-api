/**
 * Copyright 2014 Nick Rogers
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

package uk.ac.cam.cl.dtg.segue.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;

/**
 * Test class for the facebook authenticator class.
 *
 */
public abstract class IOAuthAuthenticatorTest {
  protected final String clientId = "someClientId";
  protected final String clientSecret = "someClientSecret";
  protected final String callbackUri = "someCallbackUri";
  protected final String requestedScopes = "requestedScopes";
  protected final String someDomain = "http://www.somedomain.com/";
  protected final String someAuthCode = "someAuthCode";

  protected IOAuthAuthenticator authenticator;


  /**
   * Verify that the authenticator correctly identifies itself.
   */
  @Test
  public final void getAuthenticationProvider_returnsNonNullProvider() {
    assertNotNull(authenticator.getAuthenticationProvider());
  }


  /**
   * Verify that the extractAuthCode method returns the correct value.
   */
  @Test
  public final void exchangeCode_invalidToken_throwsException() {
    assertThrows(CodeExchangeException.class, () -> authenticator.exchangeCode(someAuthCode));
  }

  /**
   * Verify that the getAntiForgeryStateToken returns some non-null non-empty string.
   */
  @Test
  public final void getUserInfo_nullReference_throwsException() {
    assertThrows(NullPointerException.class, () -> authenticator.getUserInfo(null));
  }
}
