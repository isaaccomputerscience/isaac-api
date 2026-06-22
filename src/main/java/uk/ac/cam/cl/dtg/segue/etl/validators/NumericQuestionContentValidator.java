package uk.ac.cam.cl.dtg.segue.etl.validators;

import java.math.BigDecimal;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

public class NumericQuestionContentValidator implements ContentTypeValidator {

  private static final String NUMERIC_QUESTION = "Numeric Question: ";

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof IsaacNumericQuestion q)) {
      return;
    }

    validateDisplayUnitConflict(q, context);

    if (q.getChoices() != null) {
      q.getChoices().forEach(choice -> validateChoice(q, choice, context));
    }
  }

  private void validateDisplayUnitConflict(final IsaacNumericQuestion question, final IndexingContext context) {
    if (Boolean.TRUE.equals(question.getRequireUnits()) && hasDisplayUnit(question)) {
      context.registerProblem(question,
          NUMERIC_QUESTION + question.getId()
              + " has a displayUnit set but also requiresUnits! Units will be ignored for this question!");
    }
  }

  private boolean hasDisplayUnit(final IsaacNumericQuestion question) {
    return question.getDisplayUnit() != null && !question.getDisplayUnit().isEmpty();
  }

  private void validateChoice(final IsaacNumericQuestion question, final Object choice,
                              final IndexingContext context) {
    if (!(choice instanceof Quantity quantity)) {
      context.registerProblem(question,
          NUMERIC_QUESTION + question.getId()
              + " has non-Quantity Choice (" + choice + "). It must be deleted and a new Quantity Choice created.");
      return;
    }

    if (hasUnitsButNotRequired(question, quantity)) {
      context.registerProblem(question, NUMERIC_QUESTION + question.getId()
          + " has Quantity Choice with units but does not require units.");
    }

    validateQuantityValue(question, quantity, context);
  }

  private boolean hasUnitsButNotRequired(final IsaacNumericQuestion question, final Quantity quantity) {
    return !Boolean.TRUE.equals(question.getRequireUnits())
        && quantity.getUnits() != null
        && !quantity.getUnits().isEmpty();
  }

  private void validateQuantityValue(final IsaacNumericQuestion question, final Quantity quantity,
                                     final IndexingContext context) {
    try {
      new BigDecimal(quantity.getValue());
    } catch (Exception e) {
      context.registerProblem(question,
          NUMERIC_QUESTION + question.getId() + " has a Choice (" + quantity.getValue() + ") "
              + " with value that cannot be interpreted as a number. "
              + "Users will never be able to match this answer.");
    }
  }
}