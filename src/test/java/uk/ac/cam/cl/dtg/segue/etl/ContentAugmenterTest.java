package uk.ac.cam.cl.dtg.segue.etl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;

class ContentAugmenterTest {

  private final ContentAugmenter augmenter = new ContentAugmenter();

  @Test
  void flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned() {
    final int numChildLevels = 5;
    final int numNodes = numChildLevels + 1;

    Set<Content> elements = new HashSet<>();
    Content rootNode = createContentHierarchy(numChildLevels, elements);

    Set<Content> contents = augmenter.flattenContentObjects(rootNode);

    assertEquals(numNodes, contents.size());

    for (Content c : contents) {
      boolean containsElement = elements.contains(c);
      assertTrue(containsElement);
      elements.remove(c);
    }

    assertEquals(0, elements.size());
  }

  @Test
  void flattenContentObjects_singleNode_returnsSingleElement() {
    Content singleContent = createEmptyContentElement(new LinkedList<>(), "single");

    Set<Content> flattened = augmenter.flattenContentObjects(singleContent);

    assertEquals(1, flattened.size());
    assertTrue(flattened.contains(singleContent));
  }

  /**
   * Test flattenContentObjects with direct children.
   */
  @Test
  void flattenContentObjects_withDirectChildren_returnsAllNodes() {
    Content child1 = createEmptyContentElement(new LinkedList<>(), "child1");
    Content child2 = createEmptyContentElement(new LinkedList<>(), "child2");

    List<ContentBase> children = new LinkedList<>();
    children.add(child1);
    children.add(child2);

    Content parent = createEmptyContentElement(children, "parent");

    Set<Content> flattened = augmenter.flattenContentObjects(parent);

    assertEquals(3, flattened.size());
    assertTrue(flattened.contains(parent));
    assertTrue(flattened.contains(child1));
    assertTrue(flattened.contains(child2));
  }

  @Test
  void augmentChildContent_withContent_returnsAugmentedContent() {
    Content child = createEmptyContentElement(new LinkedList<>(), "childId");

    Content result = augmenter.augmentChildContent(child, "test.json", "parent_123", true);

    assertEquals(child, result);
  }

  @Test
  void collateSearchableContent_withContent_returnsNonEmptyString() {
    Content content = createEmptyContentElement(new LinkedList<>(), "testId");
    content.setTitle("Test Title");
    content.setTags(Set.of("tag1", "tag2"));

    StringBuilder builder = new StringBuilder();
    augmenter.collateSearchableContent(content, builder);

    String result = builder.toString();
    assertFalse(result.isEmpty());
  }

  private Content createContentHierarchy(final int numLevels, final Set<Content> flatSet) {
    List<ContentBase> children = new LinkedList<>();

    if (numLevels > 0) {
      Content child = createContentHierarchy(numLevels - 1, flatSet);
      children.add(child);
    }

    Content content = createEmptyContentElement(children, String.format("%d", numLevels));
    flatSet.add(content);
    return content;
  }

  private Content createEmptyContentElement(final List<ContentBase> children, final String id) {
    return new Content(id, "", "", "", "", "", "", "", children, "", "", new LinkedList<>(),
        false, false, new HashSet<>(), 1);
  }
}