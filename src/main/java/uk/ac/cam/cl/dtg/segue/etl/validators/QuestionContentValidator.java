package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class QuestionContentValidator implements ContentTypeValidator {

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof Question q)) {
      return;
    }
    if (q.getId() == null) {
      context.registerProblem(q, "Question: " + q.getTitle() + " is missing an ID field and cannot be used.");
    }
  }
}