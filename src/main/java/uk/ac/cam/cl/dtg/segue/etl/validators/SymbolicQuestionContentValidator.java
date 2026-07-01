package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Formula;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class SymbolicQuestionContentValidator implements ContentTypeValidator {

  private static final String SYMBOLIC_QUESTION = "Symbolic Question: ";

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof IsaacSymbolicQuestion q)) {
      return;
    }
    if (q.getChoices() != null) {
      q.getChoices().forEach(choice -> validateChoice(q, choice, context));
    }
  }

  private void validateChoice(final IsaacSymbolicQuestion question, final Object choice,
                              final IndexingContext context) {
    if (!(choice instanceof Formula f)) {
      context.registerProblem(question, SYMBOLIC_QUESTION + question.getId()
          + " does not only have Formula choices.");
      return;
    }

    if (hasBackslash(f)) {
      context.registerProblem(question, SYMBOLIC_QUESTION + question.getId()
          + " has formula containing backslash.");
    }

    if (isEmptyExpression(f)) {
      context.registerProblem(question, SYMBOLIC_QUESTION + question.getId()
          + " has formula with empty python expression.");
    }
  }

  private boolean hasBackslash(final Formula formula) {
    return formula.getPythonExpression() != null && formula.getPythonExpression().contains("\\");
  }

  private boolean isEmptyExpression(final Formula formula) {
    String expr = formula.getPythonExpression();
    return expr == null || expr.isEmpty();
  }
}