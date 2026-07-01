package uk.ac.cam.cl.dtg.segue.etl.validators;

import java.util.stream.Collectors;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.etl.ContentAugmenter;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

/**
 * Validates general content structure issues not specific to any content type.
 */
public class GeneralContentValidator implements ContentTypeValidator {

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    // Content ID structural rules (null/empty skipped, illegal '.' chars, max length) are enforced as hard
    // gates at cache time in ContentGitLoader#validateAndCacheContent, so they are not re-checked here.
    // In particular, most content blocks legitimately have no ID, so flagging that would flood the report.
    validateValueWithChildren(content, context);
    validateExpandableContent(content, context);
  }

  private void validateValueWithChildren(final Content content, final IndexingContext context) {
    if (content.getValue() != null && content.getChildren() != null && !content.getChildren().isEmpty()) {
      String id = content.getId();
      String firstLine = "Content";
      if (id != null) {
        firstLine = "Content (" + id + ")";
      }
      context.registerProblem(content,
          firstLine + " has both a value and children. This is not recommended, and may cause display issues.");
    }
  }

  private void validateExpandableContent(final Content content, final IndexingContext context) {
    validateUnsupportedTypeExpandable(content, context);
    validateNestedExpandables(content, context);
  }

  private void validateUnsupportedTypeExpandable(final Content content, final IndexingContext context) {
    if (null != content.getExpandable() && content.getExpandable()
        && (null == content.getLayout() || !content.getLayout().equals("tabs"))
        && !(content instanceof CodeSnippet)) {
      context.registerProblem(content,
          "Content of type " + content.getType() + " is marked as expandable, but we do not support expanding "
              + "this type of content yet. If this is a HTML table, use class='expandable' in the table tag instead.");
    }
  }

  private void validateNestedExpandables(final Content content, final IndexingContext context) {
    if ((null != content.getLayout() && content.getLayout().equals("tabs") || content instanceof CodeSnippet)
        && null != content.getChildren()) {
      ContentAugmenter augmenter = new ContentAugmenter();
      String expandableChildrenLog = augmenter.flattenContentObjects(content).stream()
          .filter(child -> child != content && null != child.getExpandable()
              && Boolean.TRUE.equals(child.getExpandable()))
          .map(child -> null != child.getType() ? child.getType() : "undefined")
          .collect(Collectors.joining(","));

      if (!expandableChildrenLog.isEmpty()) {
        context.registerProblem(content,
            "Content of type " + content.getType() + " is potentially expandable, but has expandable children "
                + "of the following types: " + expandableChildrenLog + ". These children will have their "
                + "expandable property disabled since we cannot handle nested expandable content. Please make sure "
                + "the parent content block is marked as expandable instead, and that its children blocks have the "
                + "expandable property disabled.");
      }
    }
  }
}