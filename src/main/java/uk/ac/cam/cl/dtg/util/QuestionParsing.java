package uk.ac.cam.cl.dtg.util;

import com.google.api.client.util.Maps;
import com.opencsv.CSVWriter;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.PagesFacade;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacParsonsQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacRegexMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacReorderQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicLogicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeSnippetDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FigureDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.MediaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.FAST_TRACK_QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class QuestionParsing {
    private static final String destinationFilepath = "";
    private static final List<String> nonQuestionTypes = List.of("content", "figure", "image", "media", "codeSnippet");
    private static final List<String> ignoredTypes = List.of("glossaryTerm");
    private static final String[] csvHeadings = new String[]{"PageId|QuestionId", "Type", "Title", "Question", "Choices", "Answer", "Hints", "SubjectCategory", "Stage", "Board", "Difficulty", "Source"};
    private static final Logger log = LoggerFactory.getLogger(PagesFacade.class);

    // Entry point function, retrieve and export
    public static void retrieveAndExportQuestionsToCsv(PagesFacade pagesFacade) {
        try {
            retrieveAndExportQuestionsToCsv(pagesFacade, destinationFilepath);
        } catch (ContentManagerException e) {
            log.error("Unexpected error!", e);
//            throw new RuntimeException(e);
        }
    }
    public static void retrieveAndExportQuestionsToCsv(PagesFacade pagesFacade, String destinationFilePath) throws ContentManagerException {
            List<ContentDTO> questionsList = getQuestionsObjectList(pagesFacade);
            exportQuestionsCsvFromQuestionPageList(pagesFacade, destinationFilePath, questionsList);
    }

    // Provides an array of only the page ids
    public static String[] getQuestionPageIdsList(PagesFacade pagesFacade) throws ContentManagerException {
        return getQuestionsObjectList(pagesFacade).stream().map(e -> e.getId()).collect(Collectors.toList()).toArray(new String[0]);
    }

    // Retreives a list of a question pages, as the full page objects; not human-readable
    public static List<ContentDTO> getQuestionsObjectList(PagesFacade pagesFacade) throws ContentManagerException {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(QUESTION_TYPE));
        ResultsWrapper<ContentDTO> c;
        c = pagesFacade.api.findMatchingContent(pagesFacade.contentIndex,
                ContentService.generateDefaultFieldToMatch(fieldsToMatch, null), 0, 10000);
        return c.getResults();
    }

    // Transform a list of question page objects into a csv file
    public static void exportQuestionsCsvFromQuestionPageList(PagesFacade pagesFacade, String destinationFilePath, List<ContentDTO> questionPageList) {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(destinationFilePath, true))) {
            csvWriter.writeNext(csvHeadings);

            for (ContentDTO questionPage: questionPageList) {
                if (questionPage instanceof IsaacQuestionPageDTO) {

                    if (questionPage.getDeprecated() == null || questionPage.getDeprecated() != true) {
                        csvWriter.writeAll(mapQuestionPageToCsvLines((SeguePageDTO) questionPage));
                    }
                }
            }

        } catch (IOException e) {
            log.error("Unexpected IO error!", e);
//            throw new RuntimeException(e);
        }
    }

    // Retrieve question page objects from an array of ids and transform results into a csv file
    public static void exportQuestionsCsvFromIds(PagesFacade pagesFacade, String destinationFilePath, String[] questionPageIds) {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(destinationFilePath, true))) {
            csvWriter.writeNext(csvHeadings);

            for (String questionId : questionPageIds) {
                Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
                fieldsToMatch.put("type", Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));
                fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(questionId));
                HashMap userQuestionAttempts = new HashMap();
                Response response = pagesFacade.findSingleResult(fieldsToMatch, userQuestionAttempts);

                if (response.getEntity() != null && response.getEntity() instanceof IsaacQuestionPageDTO) {
                    SeguePageDTO content = (SeguePageDTO) response.getEntity();

                    if (content.getDeprecated() == null || content.getDeprecated() != true) {
                        csvWriter.writeAll(mapQuestionPageToCsvLines(content));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unexpected error!", e);
//            throw new RuntimeException(e);
        }
    }

    // Transform a question page object a csv-compatible line of into human-readable values
    private static List<String[]> mapQuestionPageToCsvLines(SeguePageDTO pageContent) {
        List<String[]> csvLines = new ArrayList();

        List<String> preceedingQuestionText = new ArrayList();
        for (ContentBaseDTO subContent : pageContent.getChildren()) {
            if (nonQuestionTypes.contains(subContent.getType())) {
                preceedingQuestionText.add(getContentText((ContentDTO) subContent));
            } else if (!ignoredTypes.contains(subContent.getType())) {
                csvLines.add(mapQuestionToCsvLine(pageContent, (ContentDTO) subContent, preceedingQuestionText));
            }
        }
        return csvLines;
    }

    private static String[] mapQuestionToCsvLine(SeguePageDTO pageContent, ContentDTO questionContent, List<String> preceedingQuestionText) {
        // "PageId|QuestionId", "Type", "Title", "Question", "Choices", "Answer", "Hints", "SubjectCategory", "Stage", "Board", "Difficulty", "Source"
        String qtype = questionContent.getType();
        List<String> csvLine = new ArrayList();
        try {
            // PageId|QuestionId
            csvLine.add(getId(pageContent, questionContent));
            // Type
            csvLine.add(qtype);
            // Title
            csvLine.add(getPageTitle(pageContent, questionContent));

            if (nonQuestionTypes.contains(qtype)) {
                csvLine.add(getContentText(questionContent));
                csvLine.add("");
                csvLine.add("");
                csvLine.add("");
            } else {
                // Question
                csvLine.add(getQuestion((QuestionDTO) questionContent, preceedingQuestionText));
                // Choices
                csvLine.add(getChoices((QuestionDTO) questionContent));
                // Answer
                csvLine.add(getAnswer((QuestionDTO) questionContent));
                // Hints
                csvLine.add(getHints((QuestionDTO) questionContent));
            }

            // Subject Category
            csvLine.add(pageContent.getTags().toString());
            // Stage, Board, Difficulty
            csvLine.addAll(getAudience(pageContent));
            // Source
            csvLine.add(pageContent.getCanonicalSourceFile());
        } catch (NullPointerException e) {
            log.error(String.format("Null pointer on question %1$s", pageContent.getId()));
        } catch (ClassCastException e) {
            log.error(String.format("Could not cast to subtype on question %1$s", pageContent.getId()));
        } catch (Exception e) {
            log.error("Unexpected error!", e);
        } finally {
            return csvLine.toArray(new String[0]);
        }
    }

    private static List<String> getAudience(ContentDTO content) {
        List<String> audienceLines = new ArrayList();
        if (content.getAudience() != null) {
            List<String> stages = new ArrayList();
            List<String> boards = new ArrayList();
            List<String> difficulties = new ArrayList();
            List<AudienceContext> audienceContexts = content.getAudience();
            for (AudienceContext audienceContext : audienceContexts) {
                if (audienceContext.getStage() != null) {
                    stages.add(String.join(", ", audienceContext.getStage().stream().map(s -> s.name()).collect(Collectors.toList())));
                } else stages.add("");
                if (audienceContext.getExamBoard() != null) {
                    boards.add(String.join(", ", audienceContext.getExamBoard().stream().map(b -> b.name()).collect(Collectors.toList())));
                } else boards.add("");
                if (audienceContext.getDifficulty() != null) {
                    difficulties.add(String.join(", ", audienceContext.getDifficulty().stream().map(d -> d.name()).collect(Collectors.toList())));
                } else boards.add("");
            }
            audienceLines.add(String.join("\n", stages));
            audienceLines.add(String.join("\n", boards));
            audienceLines.add(String.join("\n", difficulties));
        } else {
            audienceLines.add("");
            audienceLines.add("");
            audienceLines.add("");
        }
        return audienceLines;
    }

    private static String getContentText(ContentDTO content) {
        if (content instanceof FigureDTO) {
            return ((FigureDTO) content).getAltText();
        } else if (content instanceof ImageDTO) {
            return ((ImageDTO) content).getAltText();
        } else if (content instanceof MediaDTO) {
            return ((MediaDTO) content).getAltText();
        } else if (content instanceof CodeSnippetDTO) {
            return ((CodeSnippetDTO) content).getCode();
        } else {
            return content.getValue();
        }
    }

    private static String getHints(QuestionDTO questionContent) {
        List<String> qhints = new ArrayList();
        if (questionContent.getHints() != null) {
            for (ContentBaseDTO hint : questionContent.getHints()) {
                String hintText = ((ContentDTO) ((ContentDTO) hint).getChildren().get(0)).getValue();
                qhints.add(hintText);
            }
        }
        return String.join("\n", qhints);
    }

    private static String getChoices(QuestionDTO questionContent) {
        List<String> choices = new ArrayList();
        if (questionContent instanceof IsaacMultiChoiceQuestionDTO) {
            for (ChoiceDTO choice : ((IsaacQuestionBaseDTO) questionContent).getChoices()) {
                choices.add(choice.getValue());
            }
            return String.join("\n", choices);
        } else if (questionContent instanceof IsaacItemQuestionDTO
                || questionContent instanceof IsaacParsonsQuestionDTO
                || questionContent instanceof IsaacClozeQuestionDTO
                || questionContent instanceof IsaacReorderQuestionDTO
        ) {
            for (ItemDTO item : ((IsaacItemQuestionDTO) questionContent).getItems()) {
                choices.add(item.getValue());
            }
            return String.join("\n", choices);
        } else return "";
    }

    private static String getAnswer(QuestionDTO questionContent) {
        ContentBaseDTO qanswer = questionContent.getAnswer();
        List<String> subAnswers = new ArrayList();

        if (((ContentDTO) qanswer).getValue() instanceof String) {
            subAnswers.add(((ContentDTO) qanswer).getValue());
        } else {
            for (ContentBaseDTO subAnswer : ((ContentDTO) qanswer).getChildren()) {
                subAnswers.add(((ContentDTO) subAnswer).getValue());
            }
        }

        if (questionContent instanceof IsaacNumericQuestionDTO
                || questionContent instanceof IsaacMultiChoiceQuestionDTO
                || questionContent instanceof IsaacStringMatchQuestionDTO
                || questionContent instanceof IsaacFreeTextQuestionDTO
                || questionContent instanceof IsaacRegexMatchQuestionDTO
                || questionContent instanceof IsaacSymbolicLogicQuestionDTO
        ) {
            for (ChoiceDTO choice : ((IsaacQuestionBaseDTO) questionContent).getChoices()) {
                ContentDTO explanation = ((ContentDTO) choice.getExplanation());
                if (!explanation.getChildren().isEmpty())
                    subAnswers.add(String.format("%1$s - %2$s:", choice.isCorrect(), ((ContentDTO) explanation.getChildren().get(0)).getValue()));
                else
                    subAnswers.add(String.format("%1$s:", choice.isCorrect()));
                if (choice instanceof QuantityDTO && ((QuantityDTO) choice).getUnits() != null && !((QuantityDTO) choice).getUnits().isBlank())
                    subAnswers.add(String.format("  %1$s$2s", choice.getValue(), ((QuantityDTO) choice).getUnits()));
                else
                    subAnswers.add(String.format("  %1$s", choice.getValue()));
            }
        } else if (questionContent instanceof IsaacItemQuestionDTO
                || questionContent instanceof IsaacParsonsQuestionDTO
                || questionContent instanceof IsaacClozeQuestionDTO
                || questionContent instanceof IsaacReorderQuestionDTO
        ) {
            Map<String, String> itemValueMap = ((IsaacItemQuestionDTO) questionContent).getItems().stream().collect(Collectors.toMap(ItemDTO::getId, ItemDTO::getValue));
            for (ChoiceDTO choice : ((IsaacItemQuestionDTO) questionContent).getChoices()) {
                ContentDTO explanation = ((ContentDTO) choice.getExplanation());
                if (!explanation.getChildren().isEmpty())
                    subAnswers.add(String.format("%1$s - %2$s:", choice.isCorrect(), ((ContentDTO) explanation.getChildren().get(0)).getValue()));
                else
                    subAnswers.add(String.format("%1$s:", choice.isCorrect()));
                for (ItemDTO item : ((ItemChoiceDTO) choice).getItems()) {
                    subAnswers.add(String.format("  %1$s", itemValueMap.get(item.getId())));
                }
            }
        }
        return String.join("\n", subAnswers);
    }

    private static String getQuestion(QuestionDTO questionContent, List<String> preceedingQuestionText) {
        List<String> subQuestions = new ArrayList();
        subQuestions.addAll(preceedingQuestionText);
        if (questionContent.getValue() != null) {
            subQuestions.add(questionContent.getValue());
        } else {
            for (ContentBaseDTO subQuestion : questionContent.getChildren()) {
                subQuestions.add(getContentText((ContentDTO) subQuestion));
            }
        }
        return String.join("\n", subQuestions);
    }

    private static String getPageTitle(SeguePageDTO pageContent, ContentDTO questionContent) {
        if (questionContent.getTitle() != null) {
            return questionContent.getTitle();
        } else {
            return pageContent.getTitle();
        }
    }

    private static String getId(SeguePageDTO pageContent, ContentDTO questionContent) {
        if (questionContent.getId() != null) {
            return questionContent.getId();
        } else {
            return pageContent.getId();
        }
    }
}
