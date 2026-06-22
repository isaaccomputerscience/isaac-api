package uk.ac.cam.cl.dtg.segue.etl.validators;

import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class ClozeQuestionContentValidator implements ContentTypeValidator {

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof IsaacClozeQuestion q)) {
      return;
    }
    if (q.getChoices() == null) {
      return;
    }
    Integer expectedItemCount = null;
    for (Object choice : q.getChoices()) {
      if (choice instanceof ItemChoice c) {
        List<Item> items = c.getItems();
        if (items == null || items.isEmpty()) {
          context.registerProblem(q, "Cloze Question: " + q.getId() + " has choice with missing items!");
        } else {
          int itemCount = items.size();
          if (expectedItemCount == null) {
            expectedItemCount = itemCount;
          } else if (itemCount != expectedItemCount) {
            context.registerProblem(q,
                "Cloze Question: " + q.getId()
                    + " has choice with incorrect number of items! "
                    + "(Expected " + expectedItemCount + ", got " + itemCount + "!)");
          }
        }
      }
    }
  }
}