package uk.ac.cam.cl.dtg.segue.etl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCardDeck;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Media;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.content.Video;

/**
 * Augments content objects during indexing by:
 * - Building compound parent-child IDs
 * - Fixing media source paths
 * - Propagating published state to children
 * - Generating searchable content text
 * - Flattening hierarchical content into a flat set.
 */
public class ContentAugmenter {

  /**
   * Recursively flattens a content object and all its children into a single set.
   *
   * @param content the root content object
   * @return a flat set of all content objects in the hierarchy
   */
  public Set<Content> flattenContentObjects(final Content content) {
    Set<Content> flattenedContent = new java.util.HashSet<>();
    if (content != null) {
      flattenedContent.add(content);

      if (content instanceof IsaacCardDeck deck && deck.getCards() != null && !deck.getCards().isEmpty()) {
        deck.getCards().forEach(card -> {
          if (card != null) {
            flattenedContent.addAll(this.flattenContentObjects(card));
          }
        });
      }

      if (content.getChildren() != null) {
        content.getChildren().stream()
            .filter(Content.class::isInstance)
            .map(child -> this.flattenContentObjects((Content) child))
            .forEach(flattenedContent::addAll);
      }
    }

    return flattenedContent;
  }

  /**
   * Augments a content object by mutating child content objects recursively.
   * Builds compound IDs, updates media paths, and propagates published state.
   *
   * @param content             the parent content to augment
   * @param canonicalSourceFile path of the source JSON file
   * @param parentId            parent ID (maybe null for top-level)
   * @param parentPublished     parent's published state
   * @return the augmented content, or null if it should be skipped
   */
  public Content augmentChildContent(final Content content, final String canonicalSourceFile,
                                     final String parentId, final boolean parentPublished) {
    if (content == null) {
      return null;
    }

    content.setCanonicalSourceFile(canonicalSourceFile);

    String newParentId = computeParentId(parentId, content.getId());
    this.updateContentIdentifier(content, newParentId, parentPublished);
    this.augmentMediaContent(content, canonicalSourceFile, newParentId);

    if (content instanceof Question question) {
      this.augmentQuestionContent(question, canonicalSourceFile, newParentId, parentPublished);
    }

    // A Choice's explanation is a separate field (not in getChildren()), so it must be augmented
    // explicitly or it would miss compound-ID assignment and published-state propagation.
    if (content instanceof Choice choice && choice.getExplanation() instanceof Content explanation) {
      this.augmentChildContent(explanation, canonicalSourceFile, newParentId, parentPublished);
    }

    // Card-deck cards live in getCards() (not getChildren()); flattening includes them, so they must
    // be augmented here too, otherwise they are cached with bare/un-compounded IDs.
    if (content instanceof IsaacCardDeck cardDeck && cardDeck.getCards() != null) {
      cardDeck.getCards().forEach(
          card -> this.augmentChildContent(card, canonicalSourceFile, newParentId, parentPublished));
    }

    if (content.getChildren() != null) {
      content.getChildren().stream().filter(Content.class::isInstance).map(Content.class::cast).forEach(
          childContent -> this.augmentChildContent(childContent, canonicalSourceFile, newParentId,
              content.getPublished()));
    }

    return content;
  }

  /**
   * Computes the compound parent ID for a child content object.
   * Builds the hierarchical ID path using pipe separators.
   *
   * @param parentId  parent's compound ID (may be null)
   * @param contentId this content's own ID (may be null)
   * @return compound ID string, or null if both inputs are null
   */
  private String computeParentId(final String parentId, final String contentId) {
    if (parentId == null && contentId != null) {
      return contentId;
    } else if (parentId != null && contentId != null) {
      return parentId + "|" + contentId;
    }
    return parentId;
  }

  /**
   * Updates a content object's ID and related fields for indexing.
   *
   * @param content         the content object
   * @param parentId        the new parent ID (may be null)
   * @param parentPublished parent's published state
   */
  private void updateContentIdentifier(final Content content, final String parentId, final boolean parentPublished) {
    // Only rewrite the ID for content that already has one of its own. Assigning the parent's ID to
    // an id-less child would collapse all id-less siblings (and the parent) onto the same ID, producing
    // spurious "Duplicate ID" failures. Leaving them null means they are skipped at cache time, as before.
    if (content.getId() != null && parentId != null) {
      content.setId(parentId);
    }
    if (!parentPublished) {
      content.setPublished(false);
    }
  }

  /**
   * Augments media content by fixing source paths and assigning IDs.
   *
   * @param content             the content containing media
   * @param canonicalSourceFile source file path
   * @param parentId            parent ID for generating media IDs
   */
  private void augmentMediaContent(final Content content, final String canonicalSourceFile, final String parentId) {
    if (content instanceof Media media) {
      String src = media.getSrc();
      if (src != null && !src.isEmpty()) {
        media.setSrc(this.fixMediaSrc(canonicalSourceFile, src));
      }
      // Assign ID to media without one, based on parent ID and media source
      if (media.getId() == null && media.getSrc() != null && parentId != null) {
        media.setId(parentId + "|" + Base64.encodeBase64String(media.getSrc().getBytes()));
      }
    }

    if (content instanceof Video video) {
      String src = video.getSrc();
      if (src != null && !src.isEmpty()) {
        video.setSrc(this.fixMediaSrc(canonicalSourceFile, src));
      }
    }

    this.augmentMediaFieldsViaReflection(content, canonicalSourceFile);
  }

  /**
   * Collates all searchable text content (title and value), recursing through the child hierarchy so
   * that body text is captured. The result populates {@code searchableContent}, which backs the
   * site-wide body-text search.
   *
   * @param content the content to process
   * @param builder the StringBuilder to append to
   */
  public void collateSearchableContent(final Content content, final StringBuilder builder) {
    if (content == null) {
      return;
    }

    if (content.getTitle() != null && !content.getTitle().isEmpty()) {
      builder.append(content.getTitle()).append("\n");
    }

    if (content.getValue() != null && !content.getValue().isEmpty()) {
      builder.append(content.getValue()).append("\n");
    }

    if (content.getChildren() != null) {
      content.getChildren().stream()
          .filter(Content.class::isInstance)
          .map(Content.class::cast)
          .forEach(child -> this.collateSearchableContent(child, builder));
    }
  }

  /**
   * Augments media fields via reflection to fix paths on Media objects stored in other fields.
   *
   * @param content             the content object
   * @param canonicalSourceFile source file path
   */
  @SuppressWarnings("java:S3011")
  private void augmentMediaFieldsViaReflection(final Content content, final String canonicalSourceFile) {
    if (content == null) {
      return;
    }

    try {
      Field[] fields = content.getClass().getDeclaredFields();
      for (Field field : fields) {
        field.setAccessible(true);
        Object fieldValue = field.get(content);
        processFieldValue(fieldValue, canonicalSourceFile);
      }
    } catch (IllegalAccessException e) {
      // Ignore reflection errors
    }
  }

  private void processFieldValue(final Object fieldValue, final String canonicalSourceFile) {
    if (fieldValue instanceof Media media) {
      fixMediaPath(media, canonicalSourceFile);
    } else if (fieldValue instanceof List<?> list) {
      list.stream()
          .filter(Media.class::isInstance)
          .map(Media.class::cast)
          .forEach(media -> fixMediaPath(media, canonicalSourceFile));
    }
  }

  private void fixMediaPath(final Media media, final String canonicalSourceFile) {
    String src = media.getSrc();
    if (src != null && !src.isEmpty()) {
      media.setSrc(this.fixMediaSrc(canonicalSourceFile, src));
    }
  }

  /**
   * Augments question-related content.
   *
   * @param question        the question
   * @param sourceFile      source file path
   * @param newParentId     new parent ID
   * @param parentPublished parent's published state
   */
  private void augmentQuestionContent(final Question question, final String sourceFile, final String newParentId,
                                      final boolean parentPublished) {
    this.augmentHints(question, sourceFile, newParentId, parentPublished);
    this.augmentAnswerContent(question, sourceFile, newParentId, parentPublished);
    this.augmentFeedbackContent(question, sourceFile, newParentId, parentPublished);
    this.augmentChoiceQuestionContent(question, sourceFile, newParentId, parentPublished);
  }

  /**
   * Augments hint content within a question.
   *
   * @param question        the question
   * @param sourceFile      source file path
   * @param newParentId     new parent ID
   * @param parentPublished parent's published state
   */
  private void augmentHints(final Question question, final String sourceFile, final String newParentId,
                            final boolean parentPublished) {
    if (question.getHints() != null) {
      question.getHints().stream()
          .filter(Content.class::isInstance)
          .forEach(hint -> this.augmentChildContent((Content) hint, sourceFile, newParentId, parentPublished));
    }
  }

  /**
   * Augments answer content within a question.
   *
   * @param question        the question
   * @param sourceFile      source file path
   * @param newParentId     new parent ID
   * @param parentPublished parent's published state
   */
  private void augmentAnswerContent(final Question question, final String sourceFile, final String newParentId,
                                    final boolean parentPublished) {
    if (question.getAnswer() instanceof Content answer && answer.getChildren() != null) {
      answer.getChildren().stream()
          .filter(Content.class::isInstance)
          .forEach(child -> this.augmentChildContent((Content) child, sourceFile, newParentId, parentPublished));
    }
  }

  /**
   * Augments feedback content within a question.
   *
   * @param question        the question
   * @param sourceFile      source file path
   * @param newParentId     new parent ID
   * @param parentPublished parent's published state
   */
  private void augmentFeedbackContent(final Question question, final String sourceFile, final String newParentId,
                                      final boolean parentPublished) {
    Content defaultFeedback = question.getDefaultFeedback();
    if (defaultFeedback != null && defaultFeedback.getChildren() != null) {
      defaultFeedback.getChildren().stream()
          .filter(Content.class::isInstance)
          .forEach(child -> this.augmentChildContent((Content) child, sourceFile, newParentId, parentPublished));
    }
  }

  /**
   * Augments choice question content.
   *
   * @param question        the question
   * @param sourceFile      source file path
   * @param newParentId     new parent ID
   * @param parentPublished parent's published state
   */
  private void augmentChoiceQuestionContent(final Question question, final String sourceFile,
                                            final String newParentId, final boolean parentPublished) {
    if (question instanceof ChoiceQuestion choiceQuestion
        && choiceQuestion.getChoices() != null) {
      choiceQuestion.getChoices()
          .forEach(choice -> this.augmentChildContent(choice, sourceFile, newParentId, parentPublished));
    }
  }

  /**
   * Fixes media source paths to be relative to the source directory.
   *
   * @param canonicalSourceFile the JSON file path
   * @param originalSrc         the original media source path
   * @return the corrected media source path
   */
  private String fixMediaSrc(final String canonicalSourceFile, final String originalSrc) {
    if (originalSrc != null && (originalSrc.startsWith("http://") || originalSrc.startsWith("https://")
        || originalSrc.startsWith("/assets/"))) {
      return originalSrc;
    }
    return FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + originalSrc, true);
  }
}
