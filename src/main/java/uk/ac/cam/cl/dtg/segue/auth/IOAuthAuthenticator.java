/**
 * Copyright 2014 Nick Rogers & Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.auth;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;

/**
 * This interface defines the required methods for an oauth 1 or oauth 2
 * provider.
 * 
 */
public interface IOAuthAuthenticator extends IFederatedAuthenticator {

	/**
	 * Step 2 of OAUTH - Get authorisation code from oauth provider using the
	 * response URL they have provided.
	 * 
	 * @param url
	 *            containing the authorisation code
	 * @return the extracted authorisation code.
	 */
	String extractAuthCode(String url);

	/**
	 * Step 3 of OAUTH - Exchange short term authorisation code for an access
	 * token which can be used to authenticate the user.
	 * <p>
	 * This method will contact the oauth provider directly to exchange the
	 * authorisation code for a token.
	 * <p>
	 * This method returns an internal reference which will allow it to look up
	 * the 3rd party token. e.g. if you call an IFederatedAuthenticator method
	 * getUserDetails - it will require this internal reference.
	 * 
	 * @param authorizationCode
	 *            as provided by the getAuthCode method.
	 * @return String containing a reference number generated by this
	 *         IOAuth2Authenticator that allows us to use
	 *         FederatedAuthentication methods that require an internal
	 *         reference number.
	 * @throws CodeExchangeException
	 */
	String exchangeCode(String authorizationCode) throws CodeExchangeException;
}
