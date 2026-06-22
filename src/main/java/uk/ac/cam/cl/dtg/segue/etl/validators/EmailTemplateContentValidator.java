package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.EmailTemplate;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class EmailTemplateContentValidator implements ContentTypeValidator {

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof EmailTemplate et)) {
      return;
    }
    if (et.getPlainTextContent() == null || et.getPlainTextContent().isEmpty()) {
      context.registerProblem(et, "Email template should always have plain text content field");
    }
  }
}