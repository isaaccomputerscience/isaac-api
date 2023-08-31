/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.UserQuestionInformation;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TimeInterval.*;

/**
 * StatisticsManager.
 * TODO this file is a mess... it needs refactoring.
 */
public class StatisticsManager implements IStatisticsManager {
    private UserAccountManager userManager;
    private ILogManager logManager;
    private SchoolListReader schoolManager;
    private final GitContentManager contentManager;
    private final String contentIndex;
    private GroupManager groupManager;
    private QuestionManager questionManager;
    private ContentSummarizerService contentSummarizerService;
    private IUserStreaksManager userStreaksManager;

    private Cache<String, Object> longStatsCache;

    private static final Logger log = LoggerFactory.getLogger(StatisticsManager.class);
    private static final String GENERAL_STATS = "GENERAL_STATS";
    private static final String SCHOOL_STATS = "SCHOOL_STATS";
    private static final String LOCATION_STATS = "LOCATION_STATS";
    private static final int LONG_STATS_EVICTION_INTERVAL_MINUTES = 720; // 12 hours
    private static final long LONG_STATS_MAX_ITEMS = 20;


    /**
     * StatisticsManager.
     *
     * @param userManager
     *            - to query user information
     * @param logManager
     *            - to query Log information
     * @param schoolManager
     *            - to query School information
     * @param contentManager
     *            - to query live version information
     * @param contentIndex
     *            - index string for current content version
     * @param groupManager
     *            - so that we can see how many groups we have site wide.
     * @param questionManager
     *            - so that we can see how many questions were answered.
     * @param contentSummarizerService
     *            - to produce content summary objects
     * @param userStreaksManager
     *            - to notify users when their answer streak changes
     */
    @Inject
    public StatisticsManager(final UserAccountManager userManager, final ILogManager logManager,
                             final SchoolListReader schoolManager, final GitContentManager contentManager,
                             @Named(CONTENT_INDEX) final String contentIndex,
                             final GroupManager groupManager, final QuestionManager questionManager,
                             final ContentSummarizerService contentSummarizerService,
                             final IUserStreaksManager userStreaksManager) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.schoolManager = schoolManager;

        this.contentManager = contentManager;
        this.contentIndex = contentIndex;

        this.groupManager = groupManager;
        this.questionManager = questionManager;
        this.contentSummarizerService = contentSummarizerService;
        this.userStreaksManager = userStreaksManager;

        this.longStatsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(LONG_STATS_EVICTION_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(LONG_STATS_MAX_ITEMS).build();
    }

    /**
     * Output general stats. This returns a Map of String to Object and is intended to be sent directly to a
     * serializable facade endpoint.
     *
     * @return an ImmutableMap{@literal <String, String>} (stat name, stat value)
     * @throws SegueDatabaseException - if there is a database error.
     */
    public synchronized Map<String, Object> getGeneralStatistics()
            throws SegueDatabaseException {
        Map<String, Object> result = Maps.newHashMap();

        result.put("userGenders", userManager.getGenderCount());
        result.put("userRoles", userManager.getRoleCount());
        result.put("userSchoolInfo", userManager.getSchoolInfoStats());
        result.put("groupCount", this.groupManager.getGroupCount());

        result.put("viewQuestionEvents", logManager.getLogCountByType(IsaacServerLogType.VIEW_QUESTION.name()));
        result.put("answeredQuestionEvents", logManager.getLogCountByType(SegueServerLogType.ANSWER_QUESTION.name()));
        result.put("viewConceptEvents", logManager.getLogCountByType(IsaacServerLogType.VIEW_CONCEPT.name()));

        Map<String, Map<Role, Long>> rangedActiveUserStats = Maps.newHashMap();
        rangedActiveUserStats.put("sevenDays", userManager.getActiveRolesOverPrevious(SEVEN_DAYS));
        rangedActiveUserStats.put("thirtyDays", userManager.getActiveRolesOverPrevious(THIRTY_DAYS));
        rangedActiveUserStats.put("ninetyDays", userManager.getActiveRolesOverPrevious(NINETY_DAYS));
        rangedActiveUserStats.put("sixMonths", userManager.getActiveRolesOverPrevious(SIX_MONTHS));
        rangedActiveUserStats.put("twoYears", userManager.getActiveRolesOverPrevious(TWO_YEARS));
        result.put("activeUsersOverPrevious", rangedActiveUserStats);

        Map<String, Map<Role, Long>> rangedAnsweredQuestionStats = Maps.newHashMap();
        rangedAnsweredQuestionStats.put("sevenDays", questionManager.getAnsweredQuestionRolesOverPrevious(SEVEN_DAYS));
        rangedAnsweredQuestionStats.put("thirtyDays", questionManager.getAnsweredQuestionRolesOverPrevious(THIRTY_DAYS));
        rangedAnsweredQuestionStats.put("ninetyDays", questionManager.getAnsweredQuestionRolesOverPrevious(NINETY_DAYS));
        result.put("answeringUsersOverPrevious", rangedAnsweredQuestionStats);

        return result;
    }

    /**
     * LogCount.
     *
     * @param logTypeOfInterest
     *            - the log event that we care about.
     * @return the number of logs of that type (or an estimate).
     * @throws SegueDatabaseException
     *             if there is a problem with the database.
     */
    @Override
    public Long getLogCount(final String logTypeOfInterest) throws SegueDatabaseException {
        return this.logManager.getLogCountByType(logTypeOfInterest);
    }

    /**
     * Get an overview of all school performance. This is for analytics / admin users.
     *
     * @return list of school to statistics mapping. The object in the map is another map with keys connections,
     *         numberActiveLastThirtyDays.
     *
     * @throws UnableToIndexSchoolsException
     *             - if there is a problem getting school details.
     */
    public List<Map<String, Object>> getSchoolStatistics()
            throws UnableToIndexSchoolsException, SegueSearchException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedOutput = (List<Map<String, Object>>) this.longStatsCache
                .getIfPresent(SCHOOL_STATS);
        if (cachedOutput != null) {
            log.debug("Using cached statistics.");
            return cachedOutput;
        } else {
            log.info("Calculating School Statistics");
        }

        Map<School, List<RegisteredUserDTO>> map = getUsersBySchool();

        final String school = "school";
        final String connections = "connections";
        final String teachers = "teachers";
        final String numberActive = "numberActiveLastThirtyDays";
        final String teachersActive = "teachersActiveLastThirtyDays";
        final int thirtyDays = 30;

        Map<String, Date> lastSeenUserMap = getLastSeenUserMap();
        List<Map<String, Object>> result = Lists.newArrayList();
        for (Entry<School, List<RegisteredUserDTO>> e : map.entrySet()) {

            List<RegisteredUserDTO> teachersConnected = Lists.newArrayList();
            for (RegisteredUserDTO user : e.getValue()) {
                if (user.getRole() != null && user.getRole().equals(Role.TEACHER)) {
                    teachersConnected.add(user);
                }
            }

            result.add(ImmutableMap.of(
                school, e.getKey(),
                connections, e.getValue().size(),
                teachers, teachersConnected.size(),
                numberActive, getNumberOfUsersActiveForLastNDays(e.getValue(), lastSeenUserMap, thirtyDays).size(),
                teachersActive, getNumberOfUsersActiveForLastNDays(teachersConnected, lastSeenUserMap, thirtyDays).size()
            ));
        }

        Collections.sort(result, new Comparator<Map<String, Object>>() {
            /**
             * Descending numerical order
             */
            @Override
            public int compare(final Map<String, Object> o1, final Map<String, Object> o2) {

                if ((Integer) o1.get(numberActive) < (Integer) o2.get(numberActive)) {
                    return 1;
                }

                if ((Integer) o1.get(numberActive) > (Integer) o2.get(numberActive)) {
                    return -1;
                }

                return 0;
            }
        });

        this.longStatsCache.put(SCHOOL_STATS, result);

        return result;
    }

    /**
     * Get the number of users per school.
     *
     * @return A map of schools to integers (representing the number of registered users)
     * @throws UnableToIndexSchoolsException as per the description
     */
    public Map<School, List<RegisteredUserDTO>> getUsersBySchool() throws UnableToIndexSchoolsException, SegueSearchException {
        List<RegisteredUserDTO> users;
        Map<School, List<RegisteredUserDTO>> usersBySchool = Maps.newHashMap();

        try {
            users = userManager.findUsers(new RegisteredUserDTO());

            for (RegisteredUserDTO user : users) {
                if (user.getSchoolId() == null) {
                    continue;
                }

                School s = schoolManager.findSchoolById(user.getSchoolId());
                if (s == null) {
                    continue;
                }

                if (!usersBySchool.containsKey(s)) {
                    List<RegisteredUserDTO> userList = Lists.newArrayList();
                    userList.add(user);
                    usersBySchool.put(s, userList);
                } else {
                    usersBySchool.get(s).add(user);
                }
            }

        } catch (SegueDatabaseException | IOException e) {
            log.error("Segue database error during school frequency calculation", e);
        }

        return usersBySchool;
    }

    /**
     * Find all users belonging to a given school.
     *
     * @param schoolId
     *            - that we are interested in.
     * @return list of users.
     * @throws SegueDatabaseException
     *             - if there is a general database error
     * @throws ResourceNotFoundException
     *             - if we cannot locate the school requested.
     * @throws UnableToIndexSchoolsException
     *             - if the school list has not been indexed.
     */
    @Override
    public List<RegisteredUserDTO> getUsersBySchoolId(final String schoolId) throws ResourceNotFoundException,
            SegueDatabaseException, UnableToIndexSchoolsException, SegueSearchException {
        Validate.notNull(schoolId);

        List<RegisteredUserDTO> users;

        School s;
        try {
            s = schoolManager.findSchoolById(schoolId);
        } catch (IOException e) {
            log.error("Unable to locate school based on id.", e);
            throw new ResourceNotFoundException("Unable to locate school based on id.");
        }

        if (null == s) {
            throw new ResourceNotFoundException("The school with the id provided cannot be found.");
        }

        RegisteredUserDTO prototype = new RegisteredUserDTO();
        prototype.setSchoolId(schoolId);

        users = userManager.findUsers(prototype);

        return users;
    }

    /**
     * @return a Map of userId's to last event timestamp
     */
    public Map<String, Date> getLastSeenUserMap() {
        Map<String, Date> lastSeenMap = Maps.newHashMap();

        try {
            List<RegisteredUserDTO> users = userManager.findUsers(new RegisteredUserDTO());

            for (RegisteredUserDTO user : users) {
                if (user.getLastSeen() != null) {
                    lastSeenMap.put(user.getId().toString(), user.getLastSeen());
                } else if (user.getRegistrationDate() != null) {
                    lastSeenMap.put(user.getId().toString(), user.getRegistrationDate());
                }
            }

        } catch (SegueDatabaseException e) {
            log.error("Unable to get last seen user map", e);
        }

        return lastSeenMap;
    }

    /**
     * @param qualifyingLogEvent
     *            the string event type that will be looked for.
     * @return a map of userId's to last event timestamp
     * @throws SegueDatabaseException - if there is a problem contacting the underlying database
     */
    public Map<String, Date> getLastSeenUserMap(final String qualifyingLogEvent) throws SegueDatabaseException {
        return this.logManager.getLastLogDateForAllUsers(qualifyingLogEvent);
    }

    /**
     * getUserQuestionInformation. Produces a map that contains information about the total questions attempted,
     * (and those correct) "totalQuestionsAttempted", "totalCorrect",
     *  ,"attemptsByTag", questionAttemptsByLevelStats.
     *
     * @param userOfInterest
     *            - the user you wish to compile statistics for.
     * @return gets high level statistics about the questions a user has completed.
     * @throws SegueDatabaseException
     *             - if something went wrong with the database.
     * @throws ContentManagerException
     *             - if we are unable to look up the content.
     */
    @Override
    public Map<String, Object> getUserQuestionInformation(final RegisteredUserDTO userOfInterest)
            throws SegueDatabaseException, ContentManagerException {
        Validate.notNull(userOfInterest);

        // FIXME: there was a TODO here about tidying this up and moving it elsewhere.
        // It has been improved and tidied, but may still better belong elsewhere . . .

        UserQuestionInformation userQuestionInformation = new UserQuestionInformation();

        LocalDate now = LocalDate.now();
        LocalDate endOfAugustThisYear = LocalDate.of(now.getYear(), Month.AUGUST, NUMBER_DAYS_IN_LONG_MONTH);
        LocalDate endOfAugustLastYear = LocalDate.of(now.getYear() - 1, Month.AUGUST, NUMBER_DAYS_IN_LONG_MONTH);
        LocalDate lastDayOfPreviousAcademicYear =
                now.isAfter(endOfAugustThisYear) ? endOfAugustThisYear : endOfAugustLastYear;

        Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = questionManager.getQuestionAttemptsByUser(userOfInterest);
        Map<String, ContentDTO> questionMap = this.getQuestionMap(questionAttemptsByUser.keySet());

        // Loop through each Question attempted:
        for (Entry<String, Map<String, List<QuestionValidationResponse>>> question : questionAttemptsByUser.entrySet()) {
            ContentDTO questionContentDTO = questionMap.get(question.getKey());
            if (null == questionContentDTO) {
                log.warn(String.format("Excluding missing question (%s) from user progress statistics for user (%s)!",
                        question.getKey(), userOfInterest.getId()));
                // We no longer have any information on this question, so we won't count it towards statistics!
                continue;
            }

            userQuestionInformation.addMostRecentlyAttemptedQuestionPage(questionContentDTO); // Assumes questionAttemptsByUser is sorted!
            userQuestionInformation.incrementAttemptedQuestions();
            boolean questionIsCorrect = true;  // Are all Parts of the Question correct?
            LocalDate mostRecentCorrectQuestionPart = null;
            LocalDate mostRecentAttemptAtQuestion = null;
            // Loop through each Part of the Question:
            for (QuestionDTO questionPart : GameManager.getAllMarkableQuestionPartsDFSOrder(questionContentDTO)) {

                boolean questionPartIsCorrect = false;  // Is this Part of the Question correct?
                // Has the user attempted this part of the question at all?
                if (question.getValue().containsKey(questionPart.getId())) {
                    userQuestionInformation.incrementAttemptedQuestionParts();

                    LocalDate mostRecentAttemptAtThisQuestionPart = null;

                    // Loop through each attempt at the Question Part if they have attempted it:
                    for (QuestionValidationResponse validationResponse : question.getValue().get(questionPart.getId())) {
                        LocalDate dateAttempted = LocalDateTime.ofInstant(
                                validationResponse.getDateAttempted().toInstant(), ZoneId.systemDefault()).toLocalDate();
                        if (mostRecentAttemptAtThisQuestionPart == null || dateAttempted.isAfter(mostRecentAttemptAtThisQuestionPart)) {
                            mostRecentAttemptAtThisQuestionPart = dateAttempted;
                        }
                        if (validationResponse.isCorrect() != null && validationResponse.isCorrect()) {
                            userQuestionInformation.incrementCorrectQuestionParts();
                            if (dateAttempted.isAfter(lastDayOfPreviousAcademicYear)) {
                                userQuestionInformation.incrementCorrectQuestionPartsThisAcademicYear();
                                if (mostRecentCorrectQuestionPart == null || dateAttempted.isAfter(mostRecentCorrectQuestionPart)) {
                                    mostRecentCorrectQuestionPart = dateAttempted;
                                }
                            }
                            questionPartIsCorrect = true;
                            break; // early so that later attempts are ignored
                        }
                    }

                    // Type Stats - Count the attempt at the Question Part:
                    String questionPartType = questionPart.getType();
                    userQuestionInformation.incrementQuestionAttemptsByTypeStats(questionPartType);

                    if (mostRecentAttemptAtThisQuestionPart != null) {
                        if (mostRecentAttemptAtThisQuestionPart.isAfter(lastDayOfPreviousAcademicYear)) {
                            userQuestionInformation.incrementAttemptedQuestionPartsThisAcademicYear();
                        }

                        if (mostRecentAttemptAtQuestion == null || mostRecentAttemptAtThisQuestionPart.isAfter(mostRecentAttemptAtQuestion)) {
                            mostRecentAttemptAtQuestion = mostRecentAttemptAtThisQuestionPart;
                        }
                    }

                    // If this Question Part is correct, count this too:
                    if (questionPartIsCorrect) {
                        userQuestionInformation.incrementQuestionsCorrectByTypeStats(questionPartType);
                    }
                }

                // Correctness of whole Question: is the Question correct so far, and is this Question Part also correct?
                questionIsCorrect = questionIsCorrect && questionPartIsCorrect;
            }

            // Tag Stats - Loop through the Question's tags:
            for (String tag : questionContentDTO.getTags()) {
                userQuestionInformation.incrementQuestionsByTagStats(tag, questionIsCorrect);
            }

            // Stage and difficulty Stats
            // This is hideous, sorry
            if (questionContentDTO.getAudience() != null) {
                for (AudienceContext audience : questionContentDTO.getAudience()) {
                    userQuestionInformation.incrementQuestionsByStageAndDifficulty(audience, questionIsCorrect);
                }
            }

            // Level Stats:
            Integer questionLevelInteger = questionContentDTO.getLevel();
            String questionLevel;
            if (null == questionLevelInteger) {
                // There are questions on general pages which cannot have levels, must use a default value.
                questionLevel = "0";
            } else {
                questionLevel = questionLevelInteger.toString();
            }
            userQuestionInformation.incrementQuestionAttemptsByLevelStats(questionLevel);

            if (mostRecentAttemptAtQuestion != null && mostRecentAttemptAtQuestion.isAfter(lastDayOfPreviousAcademicYear)) {
                userQuestionInformation.incrementAttemptedQuestionsThisAcademicYear();
            }

            // If it's correct, count this globally and for the Question's level too:
            if (questionIsCorrect) {
                userQuestionInformation.incrementCorrectQuestions();
                if (mostRecentCorrectQuestionPart != null && mostRecentCorrectQuestionPart.isAfter(lastDayOfPreviousAcademicYear)) {
                    userQuestionInformation.incrementCorrectQuestionsThisAcademicYear();
                }
                userQuestionInformation.incrementQuestionsCorrectByLevelStats(questionLevel);
            } else if (userQuestionInformation.isQuestionPagesNotCompleteLessThanProgressMaxRecentQuestions()) {
                userQuestionInformation.addQuestionPageNotComplete(questionContentDTO);
            }
        }

        // Collate all the information into the JSON response as a Map:
        return userQuestionInformation.toMap(this.userManager.convertToUserSummaryObject(userOfInterest), contentSummarizerService::extractContentSummary);
    }

    /**
     * getEventLogsByDate.
     *
     * @param eventTypes
     *            - of interest
     * @param fromDate
     *            - of interest
     * @param toDate
     *            - of interest
     * @param binDataByMonth
     *            - shall we group data by the first of every month?
     * @return Map of eventType --> map of dates and frequency
     * @throws SegueDatabaseException - if there is a problem contacting the underlying database
     */
    @Override
    public Map<String, Map<org.joda.time.LocalDate, Long>> getEventLogsByDate(final Collection<String> eventTypes,
            final Date fromDate, final Date toDate, final boolean binDataByMonth) throws SegueDatabaseException {
        return this.getEventLogsByDateAndUserList(eventTypes, fromDate, toDate, null, binDataByMonth);
    }

    /**
     * getEventLogsByDate.
     *
     * @param eventTypes
     *            - of interest
     * @param fromDate
     *            - of interest
     * @param toDate
     *            - of interest
     * @param userList
     *            - user prototype to filter events. e.g. user(s) with a particular id or role.
     * @param binDataByMonth
     *            - shall we group data by the first of every month?
     * @return Map of eventType --> map of dates and frequency
     * @throws SegueDatabaseException - if there is a problem contacting the underlying database
     */
    @Override
    public Map<String, Map<org.joda.time.LocalDate, Long>> getEventLogsByDateAndUserList(final Collection<String> eventTypes,
            final Date fromDate, final Date toDate, final List<RegisteredUserDTO> userList,
            final boolean binDataByMonth) throws SegueDatabaseException {
        Validate.notNull(eventTypes);

        return this.logManager.getLogCountByDate(eventTypes, fromDate, toDate, userList, binDataByMonth);
    }

    /**
     * Calculate the number of users from the list provided that meet the criteria.
     *
     * @param users
     *            - collection of users to consider.
     * @param lastSeenUserMap
     *            - The map of user event data. UserId --> last event date.
     * @param daysFromToday
     *            - the number of days from today that should be included in the calculation e.g. 7 would be the last
     *            week's data.
     * @return a collection containing the users who meet the criteria
     */
    @Override
    public Collection<RegisteredUserDTO> getNumberOfUsersActiveForLastNDays(final Collection<RegisteredUserDTO> users,
            final Map<String, Date> lastSeenUserMap, final int daysFromToday) {

        Set<RegisteredUserDTO> qualifyingUsers = Sets.newHashSet();

        for (RegisteredUserDTO user : users) {
            Date eventDate = lastSeenUserMap.get(user.getId().toString());
            Calendar validInclusionTime = Calendar.getInstance();
            validInclusionTime.setTime(new Date());
            validInclusionTime.add(Calendar.DATE, -1 * Math.abs(daysFromToday));

            if (eventDate != null && eventDate.after(validInclusionTime.getTime())) {
                qualifyingUsers.add(user);
            }
        }

        return qualifyingUsers;
    }

    @Override
    public Map<String, Object> getDetailedUserStatistics(final RegisteredUserDTO userOfInterest) {

        // user streak info
        Map<String, Object> userStreakRecord = userStreaksManager.getCurrentStreakRecord(userOfInterest);
        userStreakRecord.put("largestStreak", userStreaksManager.getLongestStreak(userOfInterest));

        Map<String, Object> userWeeklyStreakRecord = userStreaksManager.getCurrentWeeklyStreakRecord(userOfInterest);
        userWeeklyStreakRecord.put("largestWeeklyStreak", userStreaksManager.getLongestWeeklyStreak(userOfInterest));

        Map<String, Object> result = Maps.newHashMap();
        result.put("dailyStreakRecord", userStreakRecord);
        result.put("weeklyStreakRecord", userWeeklyStreakRecord);

        return result;
    }

    /**
     * Utility method to get a load of question pages by id in one go.
     *
     * @param ids
     *            to search for
     * @return map of id to content object.
     * @throws ContentManagerException
     *             - if something goes wrong.
     */
    private Map<String, ContentDTO> getQuestionMap(final Collection<String> ids) throws ContentManagerException {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, ID_FIELDNAME + '.' + UNPROCESSED_SEARCH_FIELD_SUFFIX),
                new ArrayList<>(ids));

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, TYPE_FIELDNAME),
                Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

        // Search for questions that match the ids.
        ResultsWrapper<ContentDTO> allMatchingIds =
                this.contentManager.getContentMatchingIds(ids,
                        0, ids.size());

        List<ContentDTO> questionsForGameboard = allMatchingIds.getResults();

        Map<String, ContentDTO> questionIdToQuestionMap = Maps.newHashMap();
        for (ContentDTO content : questionsForGameboard) {
            if (content != null) {
                questionIdToQuestionMap.put(content.getId(), content);
            }
        }

        return questionIdToQuestionMap;
    }
}
