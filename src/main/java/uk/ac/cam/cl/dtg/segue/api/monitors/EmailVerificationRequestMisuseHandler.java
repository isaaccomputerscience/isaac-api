/**
 * Copyright 2015 Alistair Stead
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_MINUTE;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler to deal with email verification requests.
 * <br>
 * Preventing users from overusing this endpoint is important as some email address information is exposed for email
 * verification purposes.
 *
 * @author Alistair Stead
 *
 */
public class EmailVerificationRequestMisuseHandler implements IMisuseHandler {
  private static final Logger log = LoggerFactory.getLogger(EmailVerificationRequestMisuseHandler.class);

  public static final Integer SOFT_THRESHOLD = 2;
  public static final Integer HARD_THRESHOLD = 4;
  public static final Integer ACCOUNTING_INTERVAL = 6 * NUMBER_SECONDS_IN_MINUTE;

  @Inject
  public EmailVerificationRequestMisuseHandler() {

  }


  @Override
  public Integer getSoftThreshold() {
    return SOFT_THRESHOLD;
  }

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
