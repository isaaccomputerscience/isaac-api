/**
 * Copyright 2019 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.common.collect.Maps;
import java.util.Map;
import org.quartz.Job;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledJob;

/**
 * Class for capturing information needed to execute an SQL file on the database where segue is running.
 */
public class SegueScheduledSyncMailjetUsersJob extends SegueScheduledJob {

  public SegueScheduledSyncMailjetUsersJob(final String jobKey, final String jobGroupName, final String description,
                                           final String cronString) {
    super(jobKey, jobGroupName, description, cronString);
  }

  @Override
  public Map<String, Object> getExecutionContext() {
    return Maps.newHashMap();
  }

  @Override
  public Job getExecutableTask() {
    return new SyncMailjetUsersJob();
  }
}