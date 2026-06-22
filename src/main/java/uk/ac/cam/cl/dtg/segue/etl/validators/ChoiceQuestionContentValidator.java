package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class ChoiceQuestionContentValidator implements ContentTypeValidator {

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof ChoiceQuestion q)) {
      return;
    }
    // Plain "isaacQuestion" content is the base quick-question type, which legitimately has no choices or
    // marked answer; only the more specific ChoiceQuestion subtypes require them. Exempt it as before.
    if (content.getType() != null && content.getType().equals("isaacQuestion")) {
      return;
    }

    if (q.getChoices() == null || q.getChoices().isEmpty()) {
      context.registerProblem(q, "Question: " + q.getId() + " has no choices defined.");
      return;
    }

    boolean hasCorrectAnswer = false;
    for (Choice choice : q.getChoices()) {
      if (choice.isCorrect()) {
        hasCorrectAnswer = true;
        break;
      }
    }
    if (!hasCorrectAnswer) {
      context.registerProblem(q, "Question: " + q.getId() + " has no correct answer specified.");
    }
  }
}