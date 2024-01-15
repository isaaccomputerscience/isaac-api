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

package uk.ac.cam.cl.dtg.segue.dao;

import static org.easymock.EasyMock.createMock;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;

/**
 * Test class for the GitContentManager class.
 */
public class GitContentManagerTest {
  private GitContentManager defaultGCM;

  @BeforeEach
  public final void setUp() {
    GitDb database = createMock(GitDb.class);
    ISearchProvider searchProvider = createMock(ISearchProvider.class);
    ContentMapper contentMapper = createMock(ContentMapper.class);

    this.defaultGCM = new GitContentManager(database, searchProvider, contentMapper);
  }

  /**
   * Test that the getById method returns null if it is passed a null id.
   */
  @Test
  public void getById_invalidId_checkNullReturned() {
    String id = null;
    try {
      assertNull(defaultGCM.getContentDOById(id));
    } catch (ContentManagerException e) {
      fail("Null should be returned");
    }
  }
}
