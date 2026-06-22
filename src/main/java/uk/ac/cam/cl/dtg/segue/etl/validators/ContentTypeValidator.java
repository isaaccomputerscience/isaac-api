package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

/**
 * Validates a single content object for type-specific structural errors.
 * Implementations return without side effects if the content is not their type.
 */
public interface ContentTypeValidator {
  /**
   * Validates the given content and registers any problems found.
   *
   * @param sha     version SHA, needed for media file-existence checks
   * @param content content object to validate
   * @param context indexing context — validators write problems via context.registerProblem()
   */
  void validate(String sha, Content content, IndexingContext context);
}
