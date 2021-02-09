/**
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
package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAttemptDO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is responsible for managing and persisting quiz attempts.
 */
public class PgQuizAttemptPersistenceManager implements IQuizAttemptPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PgQuizAttemptPersistenceManager.class);

    private final MapperFacade mapper;
    private final PostgresSqlDb database;

    /**
     * Creates a new user data manager object.
     *
     * @param database
     *            - the database reference used for persistence.
     * @param mapper
     *            - An instance of an automapper that can be used for mapping to and from AssignmentDOs and DTOs.
     */
    @Inject
    public PgQuizAttemptPersistenceManager(final PostgresSqlDb database,
                                           final MapperFacade mapper) {
        this.database = database;
        this.mapper = mapper;
    }

    @Override
    public QuizAttemptDTO getByQuizAssignmentIdAndUserId(Long quizAssignmentId, Long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM quiz_attempts WHERE quiz_assignment_id = ? AND user_id = ?");
            pst.setLong(1, quizAssignmentId);
            pst.setLong(2, userId);

            ResultSet results = pst.executeQuery();
            if (results.next()) {
                return this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find quiz attempt by assignment and user", e);
        }
    }

    @Override
    public Long saveAttempt(QuizAttemptDTO attempt) throws SegueDatabaseException {
        QuizAttemptDO attemptToSave = mapper.map(attempt, QuizAttemptDO.class);

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement(
                "INSERT INTO quiz_attempts(user_id, quiz_id, quiz_assignment_id, start_date)"
                    + " VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

            pst.setLong(1, attemptToSave.getUserId());
            pst.setString(2, attemptToSave.getQuizId());

            if (attemptToSave.getQuizAssignmentId() != null) {
                pst.setLong(3, attemptToSave.getQuizAssignmentId());
            } else {
                pst.setNull(3, Types.BIGINT);
            }

            if (attemptToSave.getStartDate() != null) {
                pst.setTimestamp(4, new java.sql.Timestamp(attemptToSave.getStartDate().getTime()));
            } else {
                pst.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
            }

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save assignment.");
            }

            long attemptId;
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    attemptId = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating attempt failed, no ID obtained.");
                }

                log.debug("Saving Quiz Attempt... Quiz Attempt ID: " + attempt.getId() + " Db id : " + attemptId);
                return attemptId;
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<QuizAttemptDTO> getByQuizIdAndUserId(String quizId, Long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM quiz_attempts WHERE quiz_id = ? AND user_id = ?");
            pst.setString(1, quizId);
            pst.setLong(2, userId);

            ResultSet results = pst.executeQuery();

            List<QuizAttemptDTO> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results)));
            }

            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find quiz attempts by quiz id and user id", e);
        }
    }

    @Override
    public QuizAttemptDTO getById(Long quizAttemptId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM quiz_attempts WHERE id = ?");
            pst.setLong(1, quizAttemptId);

            ResultSet results = pst.executeQuery();
            if (results.next()) {
                return this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results));
            } else {
                throw new SQLException("No results");
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find quiz attempt by id", e);
        }
    }

    @Override
    public void deleteAttempt(QuizAttemptDTO quizAttempt) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("DELETE FROM quiz_attempts WHERE id = ?");

            pst.setLong( 1, quizAttempt.getId());

            pst.executeUpdate();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to delete quiz attempt", e);
        }
    }

    /**
     * Convert from a Quiz Attempt DO to a Quiz Attempt DTO.
     *
     * @param attemptDO
     *            - to convert
     * @return Assignment DTO
     */
    private QuizAttemptDTO convertToQuizAttemptDTO(final QuizAttemptDO attemptDO) {
        return mapper.map(attemptDO, QuizAttemptDTO.class);
    }

    /**
     * Convert from an SQL result set to an Quiz Attempt DO.
     *
     * @param sqlResults set - assumed to be at the correct position.
     * @return The QuizAttemptDO
     * @throws SQLException if we cannot access a required field.
     */
    private QuizAttemptDO convertFromSQLToQuizAttemptDO(final ResultSet sqlResults) throws SQLException {
        Long quizAssignmentId = sqlResults.getLong("quiz_assignment_id");
        if (sqlResults.wasNull()) {
            quizAssignmentId = null;
        }

        Date startDate = new Date(sqlResults.getTimestamp("start_date").getTime());

        Date completedDate = null;
        if (sqlResults.getTimestamp("completed_date") != null) {
            completedDate = new Date(sqlResults.getTimestamp("completed_date").getTime());
        }

        return new QuizAttemptDO(sqlResults.getLong("id"), sqlResults.getLong("user_id"),
            sqlResults.getString("quiz_id"), quizAssignmentId,
            startDate, completedDate);
    }
}
