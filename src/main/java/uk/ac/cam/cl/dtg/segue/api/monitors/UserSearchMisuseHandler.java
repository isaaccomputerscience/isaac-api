/**
 * Copyright 2020 Connor Holloway
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler to deal with user search requests.
 * <br>
 * Preventing users from overusing this endpoint is important as event managers could extract all users
 * by making repeat queries.
 *
 * @author Connor Holloway
 *
 */
public class UserSearchMisuseHandler implements IMisuseHandler {

  private static final Logger log = LoggerFactory.getLogger(UserSearchMisuseHandler.class);

  public static final Integer SOFT_THRESHOLD = 60;
  public static final Integer HARD_THRESHOLD = 300;
  public static final Integer ACCOUNTING_INTERVAL = NUMBER_SECONDS_IN_ONE_HOUR;

  @Inject
  public UserSearchMisuseHandler() {

  }


  @Override
  public Integer getSoftThreshold() {
    return SOFT_THRESHOLD;
  }

  /*
   * (non-Javadoc)
   *
   * @see uk.ac.cam.cl.dtg.segue.api.managers.IMisuseEvent#getHardThreshold()
   */
  @Override
  public Integer getHardThreshold() {
    return HARD_THRESHOLD;
  }

  @Override
  public Integer getAccountingIntervalInSeconds() {
    return ACCOUNTING_INTERVAL;
  }

  @Override
  public void executeSoftThresholdAction(final String message) {
    log.warn("Soft threshold limit: " + sanitiseExternalLogValue(message));
  }

  @Override
  public void executeHardThresholdAction(final String message) {
    log.error("Hard threshold limit: " + sanitiseExternalLogValue(message));
  }
}
