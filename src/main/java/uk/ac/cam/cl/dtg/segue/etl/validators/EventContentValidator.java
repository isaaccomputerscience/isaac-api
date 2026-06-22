package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class EventContentValidator implements ContentTypeValidator {

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof IsaacEventPage eventPage)) {
      return;
    }
    if (eventPage.getEndDate() == null) {
      context.registerProblem(eventPage, "Event has no end date");
    } else if (eventPage.getDate() != null && eventPage.getEndDate().isBefore(eventPage.getDate())) {
      context.registerProblem(eventPage, "Event has end date before start date");
    }
  }
}