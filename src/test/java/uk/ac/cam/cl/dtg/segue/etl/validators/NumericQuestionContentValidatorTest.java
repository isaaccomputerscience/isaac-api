package uk.ac.cam.cl.dtg.segue.etl.validators;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

class NumericQuestionContentValidatorTest {

  @Test
  void validate_withValidNumericQuestion_registersNoProblems() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setRequireUnits(false);
    question.setDisplayUnit(null);

    Quantity choice = new Quantity();
    choice.setValue("42");
    choice.setUnits(null);
    LinkedList<Choice> choices = new LinkedList<>();
    choices.add(choice);
    question.setChoices(choices);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    
    validator.validate("test-sha", question, context);

    // Verify - no problems registered
    assertTrue(problems.isEmpty());
  }

  @Test
  void validate_withDisplayUnitAndRequireUnits_registersProblem() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setRequireUnits(true);
    question.setDisplayUnit("m");

    Quantity choice = new Quantity();
    choice.setValue("42");
    LinkedList<Choice> choices = new LinkedList<>();
    choices.add(choice);
    question.setChoices(choices);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    
    validator.validate("test-sha", question, context);

    // Verify - conflict problem registered
    assertTrue(problems.containsKey(question));
  }

  @Test
  void validate_withNonQuantityChoice_registersProblem() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setRequireUnits(false);

    // Add non-Quantity choice (as Object to avoid type checking)
    LinkedList<Choice> choices = new LinkedList<>();
    Choice invalidChoice = new Choice();
    invalidChoice.setId("invalid");
    invalidChoice.setValue("invalid");
    choices.add(invalidChoice);
    question.setChoices(choices);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    
    validator.validate("test-sha", question, context);

    // Verify - non-Quantity problem registered
    assertTrue(problems.containsKey(question));
  }

  @Test
  void validate_withQuantityHavingUnitsButNotRequired_registersProblem() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setRequireUnits(false);

    Quantity choice = new Quantity();
    choice.setValue("42");
    choice.setUnits("m"); // Has units but requireUnits is false
    LinkedList<Choice> choices = new LinkedList<>();
    choices.add(choice);
    question.setChoices(choices);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    
    validator.validate("test-sha", question, context);

    assertTrue(problems.containsKey(question));
  }

  @Test
  void validate_withInvalidQuantityValue_registersProblem() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setRequireUnits(false);

    Quantity choice = new Quantity();
    choice.setValue("not-a-number"); // Invalid numeric value
    LinkedList<Choice> choices = new LinkedList<>();
    choices.add(choice);
    question.setChoices(choices);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    
    validator.validate("test-sha", question, context);

    assertTrue(problems.containsKey(question));
  }

  @Test
  void validate_withValidQuantityAndUnits_registersNoProblems() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setRequireUnits(true);
    question.setDisplayUnit(null);

    Quantity choice = new Quantity();
    choice.setValue("42.5");
    choice.setUnits("m");
    LinkedList<Choice> choices = new LinkedList<>();
    choices.add(choice);
    question.setChoices(choices);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", question, context);

    assertTrue(problems.isEmpty());
  }

  @Test
  void validate_withNullChoices_registersNoProblems() {
    
    NumericQuestionContentValidator validator = new NumericQuestionContentValidator();
    IsaacNumericQuestion question = new IsaacNumericQuestion();
    question.setId("numeric-1");
    question.setChoices(null);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", question, context);

    assertTrue(problems.isEmpty());
  }
}
