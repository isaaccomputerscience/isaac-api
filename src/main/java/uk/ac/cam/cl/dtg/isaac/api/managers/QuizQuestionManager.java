/**
 * Copyright 2021 Raspberry Pi Foundation
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static uk.ac.cam.cl.dtg.segue.api.Constants.ESCAPED_ID_SEPARATOR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ESCAPED_ID_SPLIT_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_SEPARATOR;

import com.google.api.client.util.Maps;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.mappers.MapStructMainMapper;

public class QuizQuestionManager {
  private final QuestionManager questionManager;
  private final MapStructMainMapper mapper;
  private final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager;
  private final QuizManager quizManager;
  private final QuizAttemptManager quizAttemptManager;

  private static final Logger log = LoggerFactory.getLogger(QuizQuestionManager.class);

  /**
   * Manage questions on quizzes.
   * <br>
   * Delegates some behaviour to QuestionManager for the safety of consumers who must not confuse questions with
   * quiz questions.
   *
   * @param questionManager            - for parsing and validating question answers.
   * @param mapper                     - an auto mapper to allow us to convert to and from QuestionValidationResponseDOs
   *                                         and DTOs.
   * @param quizQuestionAttemptManager - for quiz question attempt persistence.
   * @param quizManager                - for quiz sections.
   * @param quizAttemptManager         - for attempts, particularly checking attempts are completed before revealing
   *                                         feedback.
   */
  @Inject
  public QuizQuestionManager(final QuestionManager questionManager, final MapStructMainMapper mapper,
                             final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager,
                             final QuizManager quizManager, final QuizAttemptManager quizAttemptManager) {
    this.questionManager = questionManager;
    this.mapper = mapper;
    this.quizQuestionAttemptManager = quizQuestionAttemptManager;
    this.quizManager = quizManager;
    this.quizAttemptManager = quizAttemptManager;
  }

  public ChoiceDTO convertJsonAnswerToChoice(final String jsonAnswer) throws ErrorResponseWrapper {
    return questionManager.convertJsonAnswerToChoice(jsonAnswer);
  }

  public QuestionValidationResponseDTO validateAnswer(final Question question, final ChoiceDTO answerFromClientDTO)
      throws ErrorResponseWrapper {
    Response response = questionManager.validateAnswer(question, answerFromClientDTO);
    if (response.getEntity() instanceof QuestionValidationResponseDTO) {
      return (QuestionValidationResponseDTO) response.getEntity();
    } else if (response.getEntity() instanceof SegueErrorResponse) {
      throw new ErrorResponseWrapper((SegueErrorResponse) response.getEntity());
    } else {
      throw new ErrorResponseWrapper(
          new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, response.getEntity().toString()));
    }
  }

  public void recordQuestionAttempt(final QuizAttemptDTO quizAttempt,
                                    final QuestionValidationResponseDTO questionResponse)
      throws SegueDatabaseException {
    QuestionValidationResponse questionResponseDO =
        this.mapper.map(questionResponse);

    this.quizQuestionAttemptManager.registerQuestionAttempt(quizAttempt.getId(), questionResponseDO);
  }

  /**
   * This method will ensure any user question attempt information available is used to augment this question object.
   * <br>
   * It will also ensure that any personalisation of questions is affected (e.g. randomised multichoice elements).
   * <br>
   * Note: It will not do anything to related content
   *
   * @param quiz           - to augment - this object may be mutated as a result of this method. i.e. BestAttempt field
   *                             set on question DTOs.
   * @param quizAttempt    - which attempt at the quiz to get attempts for.
   * @param includeCorrect - include whether the answers are correct.
   * @return The quiz object augmented (generally a modified parameter).
   */
  public IsaacQuizDTO augmentQuestionsForUser(final IsaacQuizDTO quiz, final QuizAttemptDTO quizAttempt,
                                              final boolean includeCorrect)
      throws SegueDatabaseException {
    List<QuestionDTO> questionsToAugment = GameManager.getAllMarkableQuestionPartsDFSOrder(quiz);

    Map<QuestionDTO, QuestionValidationResponse> answerMap = getAnswerMap(quizAttempt, questionsToAugment);

    this.augmentQuestionObjectWithAttemptInformation(answerMap, includeCorrect);

    questionManager.shuffleChoiceQuestionsChoices(quizAttempt.getUserId().toString(), questionsToAugment);

    return quiz;
  }

  /**
   * Modify the quiz to contain feedback for the specified mode, and possibly the users answers and the correct answers.
   *
   * @param quizAttempt  - which attempt at the quiz to get attempts for.
   * @param quiz         - to augment - this object may be mutated as a result of this method. i.e. BestAttempt field
   *                           set on question DTOs.
   * @param feedbackMode - what level of feedback to augment with.
   * @return the augmented QuizAttemptDTO
   */
  public QuizAttemptDTO augmentFeedbackFor(final QuizAttemptDTO quizAttempt, final IsaacQuizDTO quiz,
                                           final QuizFeedbackMode feedbackMode)
      throws SegueDatabaseException, ContentManagerException {
    quizAttempt.setFeedbackMode(feedbackMode);
    if (feedbackMode == QuizFeedbackMode.NONE) {
      // No feedback, no augmentation to do.
      return quizAttempt;
    }

    // Go get the answers
    Collection<QuestionDTO> questionsToAugment = GameManager.getAllMarkableQuestionPartsDFSOrder(quiz);
    List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(quiz);

    augmentQuizTotals(quiz, questionsToAugment);

    Map<QuestionDTO, QuestionValidationResponse> answerMap = getAnswerMap(quizAttempt, questionsToAugment);

    // Augment the feedback with answers if they should be available.
    if (feedbackMode == QuizFeedbackMode.DETAILED_FEEDBACK) {
      augmentQuestionObjectWithAttemptInformation(answerMap, true);
    }

    QuizFeedbackDTO feedback = getIndividualQuizFeedback(sections, feedbackMode, questionsToAugment, answerMap);

    quiz.setIndividualFeedback(feedback);

    quizAttempt.setQuiz(quiz);

    return quizAttempt;
  }

  /**
   * Return a map of users to their individual feedback for an assignment. Also augments the quiz with totals.
   * <br>
   * Sets total and sectionTotals on the quiz object.
   *
   * @param quiz       - to augment - this object will be mutated as a result of this method. i.e. total and
   *                         sectionsTotals will be set.
   * @param assignment - the quiz assignment to get feedback for.
   * @param users      - the users to get feedback for.
   * @return a Map of Users to their associated Quiz Feedback
   */
  public Map<RegisteredUserDTO, QuizFeedbackDTO> getAssignmentTeacherFeedback(
      final IsaacQuizDTO quiz, final QuizAssignmentDTO assignment, final List<RegisteredUserDTO> users)
      throws ContentManagerException, SegueDatabaseException {
    Collection<QuestionDTO> questionsToAugment = GameManager.getAllMarkableQuestionPartsDFSOrder(quiz);
    List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(quiz);
    augmentQuizTotals(quiz, questionsToAugment);

    Set<Long> completedUserIds = quizAttemptManager.getCompletedUserIds(assignment);

    Map<Long, Map<String, List<QuestionValidationResponse>>> answers =
        quizQuestionAttemptManager.getAllAnswersForQuizAssignment(assignment.getId());

    return users.stream().collect(Collectors.toMap(user -> user, user -> {
      // Not completed.
      if (!completedUserIds.contains(user.getId())) {
        return new QuizFeedbackDTO();
      }

      // No questions attempted.
      if (!answers.containsKey(user.getId())) {
        Map<String, QuizFeedbackDTO.Mark> sectionMarks = sections.stream().collect(Collectors.toMap(
            ContentBaseDTO::getId,
            s -> QuizFeedbackDTO.Mark.notAttempted(quiz.getSectionTotals().get(s.getId()))));
        return new QuizFeedbackDTO(QuizFeedbackDTO.Mark.notAttempted(quiz.getTotal()), sectionMarks, null);
      }

      // Calculate the scores.
      Map<QuestionDTO, QuestionValidationResponse> answerMap =
          extractAnswers(questionsToAugment, answers.get(user.getId()));
      return getIndividualQuizFeedback(sections, QuizFeedbackMode.DETAILED_FEEDBACK, questionsToAugment, answerMap);
    }));
  }

  /**
   * Extract the latest attempt for each question.
   *
   * @param quizAttempt        - which attempt at the quiz to get attempts for.
   * @param questionsToAugment - list of question objects to extract the latest attempt for.
   * @return a map of question DTO to latest {@link QuestionValidationResponse}
   */
  @VisibleForTesting
  Map<QuestionDTO, QuestionValidationResponse> getAnswerMap(final QuizAttemptDTO quizAttempt,
                                                            final Collection<QuestionDTO> questionsToAugment)
      throws SegueDatabaseException {
    Map<String, List<QuestionValidationResponse>> answers =
        quizQuestionAttemptManager.getAllAnswersForQuizAttempt(quizAttempt.getId());

    return extractAnswers(questionsToAugment, answers);
  }

  /**
   * From a map of all answers, extract the latest.
   *
   * @param questionsToAugment - list of question objects to extract the latest attempt for.
   * @param answers            - map of all question ids to a list of answers in timestamp order.
   * @return a map of question DTO to latest {@link QuestionValidationResponse}
   */
  private Map<QuestionDTO, QuestionValidationResponse> extractAnswers(
      final Collection<QuestionDTO> questionsToAugment, final Map<String, List<QuestionValidationResponse>> answers) {
    Map<QuestionDTO, QuestionValidationResponse> results = new HashMap<>();

    for (QuestionDTO question : questionsToAugment) {
      List<QuestionValidationResponse> questionAttempts = answers.get(question.getId());

      QuestionValidationResponse lastResponse = null;

      if (questionAttempts != null && questionAttempts.size() > 0) {
        // The latest answer is the only answer we consider.
        lastResponse = questionAttempts.get(questionAttempts.size() - 1);
      }

      results.put(question, lastResponse);
    }

    return results;
  }

  /**
   * Modify the questions in a quiz to contain the latest answers if available.
   * <br>
   * When we say bestAttempt, we actually mean latest attempt.
   *
   * @param answerMap      Map from QuestionDTOs to the latest answer (or null if there is no latest answer).
   * @param includeCorrect Include whether the answers are correct.
   */
  @VisibleForTesting
  void augmentQuestionObjectWithAttemptInformation(
      final Map<QuestionDTO, QuestionValidationResponse> answerMap, final boolean includeCorrect) {
    answerMap.forEach((question, lastResponse) -> {
      if (lastResponse != null) {
        QuestionValidationResponseDTO lastAttempt;
        if (includeCorrect) {
          // Include full validation details.
          lastAttempt = questionManager.convertQuestionValidationResponseToDTO(lastResponse);
        } else {
          // Manual extract only the safe details (questionId, answer, date attempted).
          lastAttempt = new QuestionValidationResponseDTO();
          lastAttempt.setAnswer(mapper.map(lastResponse.getAnswer()));
          lastAttempt.setQuestionId(lastResponse.getQuestionId());
          lastAttempt.setDateAttempted(lastResponse.getDateAttempted());
        }
        question.setBestAttempt(lastAttempt);
      }
    });
  }

  /**
   * Get the feedback (marks) for an individual's answers to a quiz.
   *
   * @param quiz               The quiz of interest.
   * @param feedbackMode       What level of feedback to provide.
   * @param questionsToAugment The questions from the quiz.
   * @param answerMap          The individual's answers.
   * @return The quiz feedback.
   * @throws ContentManagerException In DEV, if there is a malformed quiz (top-level non-Sections).
   */
  @VisibleForTesting
  QuizFeedbackDTO getIndividualQuizFeedback(
      final IsaacQuizDTO quiz, final QuizFeedbackMode feedbackMode, final Collection<QuestionDTO> questionsToAugment,
      final Map<QuestionDTO, QuestionValidationResponse> answerMap) throws ContentManagerException {
    List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(quiz);
    return getIndividualQuizFeedback(sections, feedbackMode, questionsToAugment, answerMap);
  }

  /**
   * Get the feedback (marks) for an individual's answers to a quiz.
   *
   * @param sections           The sections of the quiz.
   * @param feedbackMode       What level of feedback to provide.
   * @param questionsToAugment The questions from the quiz.
   * @param answerMap          The individual's answers.
   * @return The quiz feedback.
   */
  @Nullable
  private QuizFeedbackDTO getIndividualQuizFeedback(
      final List<IsaacQuizSectionDTO> sections, final QuizFeedbackMode feedbackMode,
      final Collection<QuestionDTO> questionsToAugment,
      final Map<QuestionDTO, QuestionValidationResponse> answerMap) {
    if (feedbackMode == QuizFeedbackMode.NONE) {
      return null;
    }

    // Make a score table
    Map<String, QuizFeedbackDTO.Mark> sectionMarks =
        sections.stream().collect(Collectors.toMap(s -> s.getId(), s -> new QuizFeedbackDTO.Mark()));
    Map<String, QuizFeedbackDTO.Mark> questionMarks =
        questionsToAugment.stream().collect(Collectors.toMap(s -> s.getId(), s -> new QuizFeedbackDTO.Mark()));

    // Calculate the scores
    for (QuestionDTO question : questionsToAugment) {
      String sectionId = extractSectionIdFromQuizQuestionId(question.getId());
      QuizFeedbackDTO.Mark sectionMark = sectionMarks.get(sectionId);
      QuizFeedbackDTO.Mark questionMark = questionMarks.get(question.getId());
      if (sectionMark == null) {
        log.error("Missing test section id: {} in question {} but not in section map {}", sectionId, question,
            sections);
        continue;
      }
      QuestionValidationResponse response = answerMap.get(question);
      if (response != null) {
        if (response.isCorrect()) {
          sectionMark.setCorrect(sectionMark.getCorrect() + 1);
          questionMark.setCorrect(1);
        } else {
          sectionMark.setIncorrect(sectionMark.getIncorrect() + 1);
          questionMark.setIncorrect(1);
        }
      } else {
        sectionMark.setNotAttempted(sectionMark.getNotAttempted() + 1);
        questionMark.setNotAttempted(1);
      }
    }

    QuizFeedbackDTO.Mark overall = consolidateMarks(sectionMarks);

    QuizFeedbackDTO feedback;
    switch (feedbackMode) {
      case OVERALL_MARK:
        feedback = new QuizFeedbackDTO(overall, null, null);
        break;
      case SECTION_MARKS:
        feedback = new QuizFeedbackDTO(overall, sectionMarks, null);
        break;
      case DETAILED_FEEDBACK:
        feedback = new QuizFeedbackDTO(overall, sectionMarks, questionMarks);
        break;
      default:
        log.error("Non-exhaustive switch on feedbackMode");
        feedback = null;
    }
    return feedback;
  }

  private QuizFeedbackDTO.Mark consolidateMarks(final Map<String, QuizFeedbackDTO.Mark> scoreTable) {
    QuizFeedbackDTO.Mark result = new QuizFeedbackDTO.Mark();
    scoreTable.values().forEach(mark -> {
      result.setCorrect(result.getCorrect() + mark.getCorrect());
      result.setIncorrect(result.getIncorrect() + mark.getIncorrect());
      result.setNotAttempted(result.getNotAttempted() + mark.getNotAttempted());
    });
    return result;
  }

  private void augmentQuizTotals(final IsaacQuizDTO quiz, final Collection<QuestionDTO> questions) {
    int total = 0;
    Map<String, Integer> sectionTotals = Maps.newHashMap();
    for (QuestionDTO question : questions) {
      String sectionId = extractSectionIdFromQuizQuestionId(question.getId());
      total++;
      sectionTotals.merge(sectionId, 1, Integer::sum);
    }

    quiz.setTotal(total);
    quiz.setSectionTotals(sectionTotals);
  }

  /**
   * Extract the fully-qualified section ID from the question ID.
   * <br>
   * Note this means we extract "quizId|sectionId" from "quizId|sectionId|questionId".
   * It ends up being neater because all the section objects have ids that are fully-qualified.
   *
   * @param questionId A question ID of the form "quizId|sectionId|questionId".
   * @return A section ID of the form "quizId|sectionId".
   */
  private static String extractSectionIdFromQuizQuestionId(final String questionId) {
    String[] ids = questionId.split(ESCAPED_ID_SEPARATOR, ESCAPED_ID_SPLIT_LIMIT);
    return ids[0] + ID_SEPARATOR + ids[1];
  }
}
