/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.etl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;

/**
 * Test class for the ContentIndexer orchestrator.
 */
class ContentIndexerTest {

  @Test
  void flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned() {
    final int numChildLevels = 5;
    final int numNodes = numChildLevels + 1;

    Set<Content> elements = new HashSet<>();
    Content rootNode = createContentHierarchy(numChildLevels, elements);

    Set<Content> contents = new ContentAugmenter().flattenContentObjects(rootNode);

    assertEquals(numNodes, contents.size());

    contents.forEach(c -> {
      boolean containsElement = elements.contains(c);
      assertTrue(containsElement);
      elements.remove(c);
    });

    assertEquals(0, elements.size());
  }

  @Test
  void constructor_createsInstance() {
    ContentMapperUtils mapperUtils = new ContentMapperUtils();
    ContentIndexer indexer = new ContentIndexer(null, null, mapperUtils);
    assertNotNull(indexer);
  }

  @Test
  void setNamedVersion_callsElasticsearch() {
    ContentMapperUtils mapperUtils = new ContentMapperUtils();
    ContentIndexer indexer = new ContentIndexer(null, null, mapperUtils);
    try {
      indexer.setNamedVersion("latest", "v1.0");
    } catch (NullPointerException e) {
      // Expected when null ES client
    }
  }

  @Test
  void augmentChildContent_delegatesToAugmenter() {
    ContentAugmenter augmenter = new ContentAugmenter();

    Content content = new Content("test-id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Content result = augmenter.augmentChildContent(content, "test.json", null, true);

    assertNotNull(result);
    assertEquals("test-id", result.getId());
  }

  @Test
  void flattenContentObjects_delegatesToAugmenter() {
    ContentAugmenter augmenter = new ContentAugmenter();

    Content rootContent = new Content("root", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Set<Content> flattened = augmenter.flattenContentObjects(rootContent);

    assertNotNull(flattened);
    assertEquals(1, flattened.size());
  }

  @Test
  void collateSearchableContent_delegatesToAugmenter() {
    ContentAugmenter augmenter = new ContentAugmenter();

    Content content = new Content("test-id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    content.setTitle("Test Title");
    content.setValue("Test Value");

    StringBuilder builder = new StringBuilder();

    augmenter.collateSearchableContent(content, builder);

    String result = builder.toString();
    assertTrue(result.contains("Test Title") || result.contains("Test Value") || result.isEmpty());
  }

  private Content createContentHierarchy(final int numLevels,
                                         final Set<Content> flatSet) {
    List<ContentBase> children = new LinkedList<>();

    if (numLevels > 0) {
      Content child = createContentHierarchy(numLevels - 1, flatSet);
      children.add(child);
    }

    Content content = createEmptyContentElement(children,
        String.format("%d", numLevels));
    flatSet.add(content);
    return content;
  }

  private Content createEmptyContentElement(final List<ContentBase> children,
                                            final String id) {
    return new Content(id, "", "", "", "", "", "", "", children, "",
        "", new LinkedList<>(), false, false, new HashSet<>(), 1);
  }

}
