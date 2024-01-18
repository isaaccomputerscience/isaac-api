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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.GenericUrl;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

/**
 * Test class for the facebook authenticator class.
 *
 */
public abstract class IOAuth2AuthenticatorTest extends IOAuthAuthenticatorTest {
  protected IOAuth2Authenticator oauth2Authenticator;

  /**
   * Verify that the authenticator returns a valid authorization URL.
   *
   * @throws IOException test exception
   */
  @Test
  public final void getAuthorizationUrl_returnsNonNullUrl() throws IOException {
    String someAntiForgeryToken = "someAntiForgeryToken";
    String urlString = oauth2Authenticator.getAuthorizationUrl(someAntiForgeryToken);
    assertNotNull(urlString);
    URL url = new URL(urlString);
    assertNotNull(url.getAuthority());
  }

  /**
   * Verify that the getAntiForgeryStateToken returns some non-null non-empty string.
   */
  @Test
  public final void getAntiForgeryStateToken_returnsNonNullString() {
    String token = oauth2Authenticator.getAntiForgeryStateToken();
    assertTrue(token != null && token.length() > 0);
  }

  /**
   * Verify that the extractAuthCode method returns the correct value.
   */
  @Test
  public final void extractAuthCode_givenValidUrl_returnsCorrectCode() {
    GenericUrl url = new GenericUrl(someDomain);
    url.set("code", someAuthCode);
    String code = authenticator.extractAuthCode(url.build());
    assertEquals(someAuthCode, code);
  }
}
