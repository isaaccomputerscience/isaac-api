/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dao;

import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager.getInstantFromTimestamp;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardContentDescriptor;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * This class is responsible for managing and persisting user data.
 */
public class GameboardPersistenceManager {

  private static final Logger log = LoggerFactory.getLogger(GameboardPersistenceManager.class);
  private static final Long GAMEBOARD_TTL_MINUTES = 30L;
  private static final int GAMEBOARD_ITEM_MAP_BATCH_SIZE = 1000;

  private final PostgresSqlDb database;
  private final Cache<String, GameboardDO> gameboardNonPersistentStorage;

  private final MainObjectMapper mapper; // used for content object mapping.
  private final ObjectMapper objectMapper; // used for json serialisation

  private final GitContentManager contentManager;

  private final URIManager uriManager;

  /**
   * Creates a new user data manager object.
   *
   * @param database       the database reference used for persistence.
   * @param contentManager allows us to lookup gameboard content.
   * @param mapper         An instance of an automapper that can be used for mapping to and from GameboardDOs and DTOs.
   * @param objectMapper   An instance of an automapper that can be used for converting objects to and from json.
   * @param uriManager     so we can generate appropriate content URIs.
   */
  @Inject
  public GameboardPersistenceManager(final PostgresSqlDb database, final GitContentManager contentManager,
                                     final MainObjectMapper mapper, final ObjectMapper objectMapper,
                                     final URIManager uriManager) {
    this.database = database;
    this.mapper = mapper;
    this.contentManager = contentManager;
    this.objectMapper = objectMapper;
    this.uriManager = uriManager;
    this.gameboardNonPersistentStorage = CacheBuilder.newBuilder()
        .expireAfterAccess(GAMEBOARD_TTL_MINUTES, TimeUnit.MINUTES).<String, GameboardDO>build();
  }

  /**
   * Find a gameboard by id.
   *
   * @param gameboardId the id to search for.
   * @return the gameboard or null if we can't find it..
   * @throws SegueDatabaseException  if there is a problem accessing the database.
   */
  public GameboardDTO getGameboardById(final String gameboardId) throws SegueDatabaseException {
    return this.getGameboardById(gameboardId, true);
  }

  /**
   * Utility method to allow us to retrieve a gameboard either from temporary storage or permanent.
   *
   * @param gameboardId   gameboard to find
   * @param fullyPopulate true or false
   * @return gameboard dto or null if we cannot find the gameboard requested
   * @throws SegueDatabaseException if there is a problem with the database
   */
  private GameboardDTO getGameboardById(final String gameboardId, final boolean fullyPopulate)
      throws SegueDatabaseException {
    if (null == gameboardId) {
      return null;
    }

    // first try temporary storage
    if (this.gameboardNonPersistentStorage.getIfPresent(gameboardId) != null) {
      return this.convertToGameboardDTO(this.gameboardNonPersistentStorage.getIfPresent(gameboardId));
    }

    String query = "SELECT * FROM gameboards WHERE id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_BY_ID_GAMEBOARD_ID, gameboardId);

      try (ResultSet results = pst.executeQuery()) {

        List<GameboardDO> listOfResults = Lists.newArrayList();
        while (results.next()) {
          listOfResults.add(this.convertFromSQLToGameboardDO(results));
        }

        if (listOfResults.size() == 0) {
          return null;
        }

        if (listOfResults.size() > 1) {
          throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
              + listOfResults);
        }

        return this.convertToGameboardDTO(listOfResults.get(0), fullyPopulate);
      }
    } catch (SQLException | IOException e) {
      throw new SegueDatabaseException("Unable to find assignment by id", e);
    }
  }

  /**
   * Find a list of gameboards by their ids.
   *
   * @param gameboardIds the ids to search for.
   * @return the gameboards or null if we can't find them.
   * @throws SegueDatabaseException  if there is a problem accessing the database.
   */
  public List<GameboardDTO> getGameboardsByIds(final List<String> gameboardIds) throws SegueDatabaseException {
    return this.getGameboardsByIds(gameboardIds, true);
  }

  /**
   * Utility method to allow us to retrieve a gameboard either from temporary storage or permanent.
   *
   * @param gameboardIds  gameboard to find
   * @param fullyPopulate true or false
   * @return gameboard dto or null if we cannot find the gameboard requested
   * @throws SegueDatabaseException if there is a problem with the database
   */
  private List<GameboardDTO> getGameboardsByIds(final Collection<String> gameboardIds, final boolean fullyPopulate)
      throws SegueDatabaseException {
    if (null == gameboardIds || gameboardIds.isEmpty()) {
      return Collections.emptyList();
    }

    // First, try temporary storage
    List<GameboardDTO> cachedGameboards = new ArrayList<>();
    List<String> gameboardIdsForQuery = new ArrayList<>();
    for (String gameboardId : gameboardIds) {
      GameboardDO cachedGameboard = this.gameboardNonPersistentStorage.getIfPresent(gameboardId);
      if (null != cachedGameboard) {
        cachedGameboards.add(this.convertToGameboardDTO(cachedGameboard));
      } else {
        gameboardIdsForQuery.add(gameboardId);
      }
    }

    // Then, go for the database
    String query = "SELECT * FROM gameboards WHERE id = ANY (?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      Array gameboardIdsPreparedArray = conn.createArrayOf("varchar", gameboardIdsForQuery.toArray());
      pst.setArray(FIELD_GET_BY_IDS_GAMEBOARD_ID_LIST, gameboardIdsPreparedArray);

      try (ResultSet results = pst.executeQuery()) {
        List<GameboardDO> listOfResults = new ArrayList<>();
        while (results.next()) {
          listOfResults.add(this.convertFromSQLToGameboardDO(results));
        }

        if (listOfResults.size() == 0) {
          return null;
        }

        List<GameboardDTO> databaseGameboards =
            listOfResults.stream().map(r -> this.convertToGameboardDTO(r, fullyPopulate)).collect(Collectors.toList());

        return Stream.of(cachedGameboards, databaseGameboards).flatMap(Collection::stream).collect(Collectors.toList());
      }
    } catch (SQLException | IOException e) {
      throw new SegueDatabaseException("Unable to find assignments by ids", e);
    }
  }

  /**
   * getLiteGameboardById. This method will get a gameboard by id but not
   * resolve any fine grain details about the board. E.g. no question details
   * will be retrieved.
   *
   * @param gameboardId to retrieve.
   * @return a lightly populated gameboard.
   * @throws SegueDatabaseException if there are problems with the database.
   */
  public GameboardDTO getLiteGameboardById(final String gameboardId) throws SegueDatabaseException {
    return this.getGameboardById(gameboardId, false);
  }

  /**
   * getLiteGameboardsByIds. This method will get a list of gameboards by their ids but not
   * resolve any fine grain details about the boards. E.g. no question details
   * will be retrieved.
   *
   * @param gameboardIds to retrieve.
   * @return a list of lightly populated gameboards.
   * @throws SegueDatabaseException if there are problems with the database.
   */
  public List<GameboardDTO> getLiteGameboardsByIds(final Collection<String> gameboardIds)
      throws SegueDatabaseException {
    return this.getGameboardsByIds(gameboardIds, false);
  }

  /**
   * Keep generated gameboard in non-persistent storage.
   * <br>
   * This will be removed if the gameboard is saved to persistent storage.
   *
   * @param gameboard
   *            to temporarily store.
   * @return gameboard id
   */
  public String temporarilyStoreGameboard(final GameboardDTO gameboard) {
    this.gameboardNonPersistentStorage.put(gameboard.getId(), this.convertToGameboardDO(gameboard));

    return gameboard.getId();
  }

  /**
   * Save a gameboard to persistent storage.
   *
   * @param gameboard gameboard to save
   * @return internal database id for the saved gameboard.
   * @throws SegueDatabaseException if there is a problem saving the gameboard in the database.
   */
  public String saveGameboardToPermanentStorage(final GameboardDTO gameboard)
      throws SegueDatabaseException {
    GameboardDO gameboardToSave = mapper.map(gameboard);
    // the mapping operation won't work for the list so we should just
    // create a new one.
    gameboardToSave.setContents(Lists.newArrayList());

    // Map each question into an IsaacQuestionInfo object
    for (GameboardItem c : gameboard.getContents()) {
      gameboardToSave.getContents().add(
          new GameboardContentDescriptor(c.getId(), c.getContentType(), c.getCreationContext()));
    }

    // This operation may not be atomic due to underlying DB. Gameboard create first then link to user view second.
    try {
      this.saveGameboard(gameboardToSave);
    } catch (JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to process json while saving gameboard.", e);
    }

    // add the gameboard to the users myboards list.
    this.createOrUpdateUserLinkToGameboard(gameboardToSave.getOwnerUserId(), gameboardToSave.getId());

    // make sure that it is not still in temporary storage
    this.gameboardNonPersistentStorage.invalidate(gameboard.getId());

    return gameboardToSave.getId();
  }

  /**
   * Allows a gameboard title to be updated. (assumes persistently stored)
   *
   * @param gameboard with updated title.
   * @return new gameboard after update.
   * @throws SegueDatabaseException if there is a problem setting the title.
   */
  public GameboardDTO updateGameboardTitle(final GameboardDTO gameboard) throws SegueDatabaseException {
    // create a new user to gameboard connection.
    String query = "UPDATE gameboards SET title = ? WHERE id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_UPDATE_TITLE_TITLE, gameboard.getTitle());
      pst.setString(FIELD_UPDATE_TITLE_ID, gameboard.getId());

      int affectedRows = pst.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Updating gameboard but no rows changed");
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }

    return gameboard;
  }

  /**
   * Determine if the gameboard only exists in temporary storage.
   *
   * @param gameboardIdToTest the gameboard to check the existence of.
   * @return true if the gameboard with that id exists in permanent storage false if not.
   * @throws SegueDatabaseException if there is a database error.
   */
  public boolean isPermanentlyStored(final String gameboardIdToTest) throws SegueDatabaseException {
    boolean isATemporaryBoard = this.gameboardNonPersistentStorage.getIfPresent(gameboardIdToTest) != null;
    boolean isAPersistentBoard = this.getGameboardById(gameboardIdToTest, false) != null;

    return isAPersistentBoard && !isATemporaryBoard;
  }

  /**
   * Determines whether a given game board is already in a users my boards list. Only boards in persistent storage
   * should be linked to a user.
   *
   * @param userId
   *            to check
   * @param gameboardId
   *            to look up
   * @return true if it is false if not
   * @throws SegueDatabaseException
   *             if there is a database error
   */
  public boolean isBoardLinkedToUser(final Long userId, final String gameboardId) throws SegueDatabaseException {
    if (userId == null || gameboardId == null) {
      return false;
    }

    String query = "SELECT COUNT(*) AS TOTAL FROM user_gameboards WHERE user_id = ? AND gameboard_id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_IS_BOARD_LINKED_TO_USER_USER_ID, userId);
      pst.setObject(FIELD_IS_BOARD_LINKED_TO_USER_GAMEBOARD_ID, gameboardId);

      try (ResultSet results = pst.executeQuery()) {
        results.next();
        return results.getInt("TOTAL") == 1;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Create a link between a user and a gameboard or update the last visited date.
   *
   * @param userId      userId to link
   * @param gameboardId gameboard to link
   * @throws SegueDatabaseException if there is a problem persisting the link in the database.
   */
  public void createOrUpdateUserLinkToGameboard(final Long userId, final String gameboardId)
      throws SegueDatabaseException {

    // Connect user to gameboard, Postgres UPSERT syntax on insert conflict:
    String query = "INSERT INTO user_gameboards(user_id, gameboard_id, created, last_visited) VALUES (?, ?, ?, ?)"
        + " ON CONFLICT ON CONSTRAINT user_gameboard_composite_key"
        + " DO UPDATE SET last_visited=EXCLUDED.last_visited;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_LINK_USER_USER_ID, userId);
      pst.setString(FIELD_LINK_USER_GAMEBOARD_ID, gameboardId);
      pst.setTimestamp(FIELD_LINK_USER_CREATED, Timestamp.from(Instant.now()));
      pst.setTimestamp(FIELD_LINK_USER_LAST_VISITED, Timestamp.from(Instant.now()));

      log.debug("Saving gameboard to user relationship...");
      int affectedRows = pst.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating/updating user link to gameboard failed, no rows changed");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Allows a link between users and a gameboard to be destroyed.
   *
   * @param userId      users id.
   * @param gameboardId gameboard ids
   * @throws SegueDatabaseException if there is an error during the delete operation.
   */
  public void removeUserLinkToGameboard(final Long userId, final Collection<String> gameboardId)
      throws SegueDatabaseException {
    StringBuilder params = new StringBuilder();
    params.append("?");
    for (int i = 1; i < gameboardId.size(); i++) {
      params.append(",?");
    }
    String query = String.format("DELETE FROM user_gameboards WHERE user_id = ? AND gameboard_id IN (%s)", params);

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_REMOVE_USER_LINK_USER_ID, userId);

      int index = FIELD_REMOVE_USER_LINK_GAMEBOARD_IDS_INITIAL_INDEX;
      for (String id : gameboardId) {
        pst.setString(index, id);
        index++;
      }

      pst.execute();
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Retrieve all gameboards (without underlying Gameboard Items) for a given
   * user.
   *
   * @param user to search for
   * @return gameboards as a list - note these gameboards will not have the
   *         questions fully populated as it is expected only summary objects
   *         are required.
   * @throws SegueDatabaseException if there is an error when accessing the database.
   */
  public List<GameboardDTO> getGameboardsByUserId(final RegisteredUserDTO user) throws SegueDatabaseException {
    // find all gameboards related to this user.
    List<GameboardDO> listOfResults = Lists.newArrayList();
    Map<String, Instant> lastVisitedDate = Maps.newHashMap();

    String query = "SELECT * FROM gameboards INNER JOIN user_gameboards"
        + " ON gameboards.id = user_gameboards.gameboard_id WHERE user_gameboards.user_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_GAMEBOARDS_BY_USER_USER_ID, user.getId());

      try (ResultSet results = pst.executeQuery()) {
        while (results.next()) {
          GameboardDO gameboard = this.convertFromSQLToGameboardDO(results);
          listOfResults.add(gameboard);
          lastVisitedDate.put(gameboard.getId(), getInstantFromTimestamp(results, "last_visited"));
        }
      }
    } catch (SQLException | IOException e) {
      throw new SegueDatabaseException("Unable to find assignment by id", e);
    }

    List<GameboardDTO> gameboardDTOs = this.convertToGameboardDTOs(listOfResults, false);

    // we need to augment each gameboard with its visited date.
    for (GameboardDTO gameboardDTO : gameboardDTOs) {
      gameboardDTO.setLastVisited(lastVisitedDate.get(gameboardDTO.getId()));
    }

    return gameboardDTOs;
  }

  /**
   * Find the list of invalid question ids.
   *
   * @param gameboardDTO to check
   * @return a List containing the ideas of any invalid or inaccessible questions - the list will be empty if none.
   */
  public List<String> getInvalidQuestionIdsFromGameboard(final GameboardDTO gameboardDTO) {
    GameboardDO gameboardDO = this.convertToGameboardDO(gameboardDTO);

    // build query the db to get full question information
    List<GitContentManager.BooleanSearchClause> fieldsToMap = Lists.newArrayList();

    fieldsToMap.add(new GitContentManager.BooleanSearchClause(
        Constants.ID_FIELDNAME + '.' + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, Constants.BooleanOperator.OR,
        gameboardDO.getContents().stream().map(GameboardContentDescriptor::getId).collect(Collectors.toList())));

    fieldsToMap.add(new GitContentManager.BooleanSearchClause(
        TYPE_FIELDNAME, Constants.BooleanOperator.AND, List.of(QUESTION_TYPE)));

    // Search for questions that match the ids.
    ResultsWrapper<ContentDTO> results;
    try {
      results = this.contentManager.findByFieldNames(
          fieldsToMap, 0, gameboardDO.getContents().size());
    } catch (ContentManagerException e) {
      results = new ResultsWrapper<ContentDTO>();
      log.error("Unable to select questions for gameboard.", e);
    }

    List<ContentDTO> questionsForGameboard = results.getResults();

    // Map each Content object into an GameboardItem object
    Map<String, GameboardItem> gameboardReadyQuestions = Maps.newHashMap();

    for (ContentDTO c : questionsForGameboard) {
      GameboardItem questionInfo = mapper.map(c, GameboardItem.class);
      questionInfo.setUri(uriManager.generateApiUrl(c));
      gameboardReadyQuestions.put(c.getId(), questionInfo);
    }

    List<String> errors = Lists.newArrayList();

    for (GameboardContentDescriptor contentDescriptor : gameboardDO.getContents()) {
      // There is a possibility that the question cannot be found any more for some reason
      // In this case we will simply pretend it isn't there.
      GameboardItem item = gameboardReadyQuestions.get(contentDescriptor.getId());
      if (null == item) {
        errors.add(contentDescriptor.getId());
      }
    }

    return errors;
  }

  /**
   * Attempt to improve performance of getting gameboard items in a batch.
   * <br>
   * This method will attempt to ensure that all gameboards provided have their associated
   * gameboard items populated with meaningful titles.
   *
   * @param gameboards list of gameboards to fully augment.
   * @return augmented gameboards as per inputted list.
   */
  public List<GameboardDTO> augmentGameboardItems(final List<GameboardDTO> gameboards) {
    Set<GameboardContentDescriptor> contentDescriptors = Sets.newHashSet();
    Map<String, List<String>> gameboardToQuestionsMap = Maps.newHashMap();

    // go through all game boards working out the set of question ids.
    for (GameboardDTO game : gameboards) {
      List<GameboardContentDescriptor> gameboardContentDescriptors = getContentDescriptors(game);
      contentDescriptors.addAll(gameboardContentDescriptors);
      gameboardToQuestionsMap.put(game.getId(), gameboardContentDescriptors.stream()
          .map(GameboardContentDescriptor::getId).collect(Collectors.toList()));
    }

    if (contentDescriptors.isEmpty()) {
      log.info("No question ids found; returning original gameboard without augmenting.");
      return gameboards;
    }

    Map<String, GameboardItem> gameboardReadyQuestions = getGameboardItemMap(Lists.newArrayList(contentDescriptors));

    for (GameboardDTO game : gameboards) {
      // empty and re-populate the gameboard dto with fully augmented gameboard items.
      game.setContents(new ArrayList<GameboardItem>());
      for (String questionId : gameboardToQuestionsMap.get(game.getId())) {
        // There is a possibility that the question cannot be found any more for some reason
        // In this case we will simply pretend it isn't there.
        GameboardItem item = gameboardReadyQuestions.get(questionId);
        if (item != null) {
          game.getContents().add(item);
        } else {
          log.warn(
              "The gameboard: {} has a reference to a question ({}) that we cannot find. Removing it from the DTO.",
              game.getId(), questionId);
        }
      }
    }
    return gameboards;
  }

  /**
   * Utility method to get a map of gameboard id to list of users who are connected to it.
   *
   * @return map of gameboard id to list of users
   * @throws SegueDatabaseException if there is a database error.
   */
  public Map<String, List<String>> getBoardToUserIdMapping() throws SegueDatabaseException {
    Map<String, List<String>> results = Maps.newHashMap();

    String query = "SELECT gameboard_id, user_id FROM user_gameboards;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet sqlResults = pst.executeQuery()
    ) {
      while (sqlResults.next()) {
        String gameboardId = sqlResults.getString("gameboard_id");
        String userId = sqlResults.getString("user_id");

        if (results.containsKey(gameboardId)) {
          results.get(gameboardId).add(userId);
        } else {
          List<String> users = Lists.newArrayList();
          users.add(userId);
          results.put(gameboardId, users);
        }
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find assignment by id", e);
    }

    return results;
  }

  /**
   * Utility function to create a gameboard item from a content DTO (Should be a question page).
   *
   * @param content           to convert
   * @param contentDescriptor a GameboardContentDescriptor with additional information to add to the returned item
   * @return the gameboard item with augmented URI.
   */
  public GameboardItem convertToGameboardItem(
      final ContentDTO content, @Nullable final GameboardContentDescriptor contentDescriptor) {
    GameboardItem questionInfo = mapper.map(content, GameboardItem.class);
    if (contentDescriptor != null) {
      questionInfo.setContentType(contentDescriptor.getContentType());
      questionInfo.setCreationContext(contentDescriptor.getContext());
    }
    questionInfo.setUri(uriManager.generateApiUrl(content));
    return questionInfo;
  }

  /**
   * saveGameboard.
   *
   * @param gameboardToSave a Gameboard DO to save to the database
   * @return the DO being persisted.
   * @throws JsonProcessingException if an error occurs while converting gameboard object properties into json strings
   * @throws SegueDatabaseException if an error occurs while updating the database record
   */
  private GameboardDO saveGameboard(final GameboardDO gameboardToSave)
      throws JsonProcessingException, SegueDatabaseException {
    String query = "INSERT INTO gameboards(id, title, contents, wildcard, wildcard_position, "
        + "game_filter, owner_user_id, creation_method, tags, creation_date)"
        + " VALUES (?, ?, ?::text::jsonb[], ?::text::jsonb, ?, ?::text::jsonb, ?, ?, ?::text::jsonb, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      List<String> contentsJsonb = Lists.newArrayList();
      for (GameboardContentDescriptor content : gameboardToSave.getContents()) {
        contentsJsonb.add(objectMapper.writeValueAsString(content));
      }
      Array contents = conn.createArrayOf("jsonb", contentsJsonb.toArray());

      pst.setObject(FIELD_SAVE_GAMEBOARD_GAMEBOARD_ID, gameboardToSave.getId());
      pst.setString(FIELD_SAVE_GAMEBOARD_TITLE, gameboardToSave.getTitle());
      pst.setArray(FIELD_SAVE_GAMEBOARD_CONTENTS, contents);
      pst.setString(FIELD_SAVE_GAMEBOARD_WILDCARD, objectMapper.writeValueAsString(gameboardToSave.getWildCard()));
      pst.setInt(FIELD_SAVE_GAMEBOARD_WILDCARD_POSITION, gameboardToSave.getWildCardPosition());
      pst.setString(FIELD_SAVE_GAMEBOARD_GAME_FILTER, objectMapper.writeValueAsString(gameboardToSave.getGameFilter()));
      pst.setLong(FIELD_SAVE_GAMEBOARD_OWNER_USER_ID, gameboardToSave.getOwnerUserId());
      pst.setString(FIELD_SAVE_GAMEBOARD_CREATION_METHOD, gameboardToSave.getCreationMethod().toString());
      pst.setString(FIELD_SAVE_GAMEBOARD_TAGS, objectMapper.writeValueAsString(gameboardToSave.getTags()));
      if (gameboardToSave.getCreationDate() != null) {
        pst.setTimestamp(FIELD_SAVE_GAMEBOARD_CREATION_DATE, Timestamp.from(gameboardToSave.getCreationDate()));
      } else {
        pst.setTimestamp(FIELD_SAVE_GAMEBOARD_CREATION_DATE, Timestamp.from(Instant.now()));
      }

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save assignment.");
      }

      log.debug("Saving gameboard... Gameboard ID: {}", gameboardToSave.getId());

      return gameboardToSave;
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Convert form a list of gameboard DOs to a list of Gameboard DTOs.
   *
   * @param gameboardDOs           to convert
   * @param populateGameboardItems true if we should fully populate the gameboard DTO with gameboard items false if
   *                                 a summary is ok do? i.e. should game board items have titles etc.
   * @return gameboard DTO
   */
  private List<GameboardDTO> convertToGameboardDTOs(final List<GameboardDO> gameboardDOs,
                                                    final boolean populateGameboardItems) {
    requireNonNull(gameboardDOs);

    List<GameboardDTO> gameboardDTOs = Lists.newArrayList();

    for (GameboardDO gameboardDO : gameboardDOs) {
      gameboardDTOs.add(this.convertToGameboardDTO(gameboardDO, populateGameboardItems));
    }

    return gameboardDTOs;
  }

  /**
   * Convert form a gameboard DO to a Gameboard DTO.
   * <br>
   * This method relies on the api to fully resolve questions.
   *
   * @param gameboardDO to convert
   * @return gameboard DTO
   */
  private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO) {
    return this.convertToGameboardDTO(gameboardDO, true);
  }

  /**
   * Convert form a gameboard DO to a Gameboard DTO.
   * <br>
   * This method relies on the api to fully resolve questions.
   *
   * @param gameboardDO            to convert
   * @param populateGameboardItems true if we should fully populate the gameboard DTO with
   *            gameboard items false if just the question ids will do?
   * @return gameboard DTO
   */
  private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO, final boolean populateGameboardItems) {
    GameboardDTO gameboardDTO = mapper.map(gameboardDO);

    if (!populateGameboardItems) {
      List<GameboardItem> listOfSparseGameItems = Lists.newArrayList();

      for (GameboardContentDescriptor contentDescriptor : gameboardDO.getContents()) {
        GameboardItem gameboardItem = GameboardItem.buildLightweightItemFromContentDescriptor(contentDescriptor);
        listOfSparseGameItems.add(gameboardItem);
      }
      gameboardDTO.setContents(listOfSparseGameItems);
      return gameboardDTO;
    }

    // Map each Content object into an GameboardItem object
    Map<String, GameboardItem> gameboardReadyQuestions = getGameboardItemMap(gameboardDO.getContents());

    // empty and repopulate the gameboard dto.
    gameboardDTO.setContents(Lists.newArrayList());
    for (GameboardContentDescriptor contentDescriptor : gameboardDO.getContents()) {
      // There is a possibility that the question cannot be found any more for some reason
      // In this case we will simply pretend it isn't there.
      GameboardItem item = gameboardReadyQuestions.get(contentDescriptor.getId());
      if (item != null) {
        gameboardDTO.getContents().add(item);
      } else {
        log.warn(String.format("The gameboard '%s' references an unavailable question '%s' - removing it from the DTO!",
            gameboardDTO.getId(), contentDescriptor.getId()));
      }
    }
    return gameboardDTO;
  }

  /**
   * Convert from a gameboard DTO to a gameboard DO.
   *
   * @param gameboardDTO DTO to convert.
   * @return GameboardDO.
   */
  private GameboardDO convertToGameboardDO(final GameboardDTO gameboardDTO) {
    GameboardDO gameboardDO = mapper.map(gameboardDTO);
    // the mapping operation won't work for the list so we should just
    // create a new one.
    gameboardDO.setContents(Lists.newArrayList());

    // Map each question into an GameboardItem object
    for (GameboardItem c : gameboardDTO.getContents()) {
      gameboardDO.getContents().add(
          new GameboardContentDescriptor(c.getId(), c.getContentType(), c.getCreationContext()));
    }

    return gameboardDO;
  }

  /**
   * Utility method to allow all gameboard related questions to be retrieved in one big batch.
   *
   * @param contentDescriptors to query for.
   * @return a map of question id to fully populated gameboard item.
   */
  private Map<String, GameboardItem> getGameboardItemMap(final List<GameboardContentDescriptor> contentDescriptors) {
    Map<String, GameboardItem> gameboardReadyQuestions = Maps.newHashMap();
    Map<String, GameboardContentDescriptor> contentDescriptorsMap = Maps.newHashMap();
    contentDescriptors.forEach(cd -> contentDescriptorsMap.put(cd.getId(), cd));

    // Batch the queries to the db to avoid the elasticsearch query clause limit of 1024
    List<List<GameboardContentDescriptor>> contentDescriptorBatches =
        Lists.partition(contentDescriptors, GAMEBOARD_ITEM_MAP_BATCH_SIZE);
    for (List<GameboardContentDescriptor> contentDescriptorBatch : contentDescriptorBatches) {
      List<String> questionsIds =
          contentDescriptorBatch.stream().map(GameboardContentDescriptor::getId).collect(Collectors.toList());
      // Search for questions that match the ids.
      ResultsWrapper<ContentDTO> results;
      try {
        results = this.contentManager.getContentMatchingIds(
            questionsIds, 0, contentDescriptorBatch.size());
      } catch (ContentManagerException e) {
        results = new ResultsWrapper<ContentDTO>();
        log.error("Unable to locate questions for gameboard. Using empty results", e);
      }

      // Map each Content object into an GameboardItem object
      List<ContentDTO> questionsForGameboard = results.getResults();
      for (ContentDTO c : questionsForGameboard) {
        GameboardItem contentInfo = this.convertToGameboardItem(c, contentDescriptorsMap.get(c.getId()));
        gameboardReadyQuestions.put(c.getId(), contentInfo);
      }
    }
    return gameboardReadyQuestions;
  }

  /**
   * Parses an sql query ResultSet into a Gameboard object.
   *
   * @param results the results from sql.
   * @return a gameboard DO.
   * @throws SQLException         if a field is missing from the results or another access error occurs
   * @throws JsonParseException   if an error occurs while parsing json strings into gameboard object properties
   * @throws JsonMappingException if an error occurs while parsing json strings into gameboard object properties
   * @throws IOException          ancestor of JsonParseException and JsonMappingException
   */
  private GameboardDO convertFromSQLToGameboardDO(final ResultSet results) throws SQLException, JsonParseException,
      JsonMappingException, IOException {
    GameboardDO gameboardDO = new GameboardDO();
    gameboardDO.setId(results.getString("id"));
    gameboardDO.setTitle(results.getString("title"));
    List<GameboardContentDescriptor> contents = Lists.newArrayList();
    for (String contentJson : (String[]) results.getArray("contents").getArray()) {
      contents.add(objectMapper.readValue(contentJson, GameboardContentDescriptor.class));
    }
    gameboardDO.setContents(contents);
    gameboardDO.setWildCard(Objects.isNull(results.getObject("wildcard")) ? null :
        objectMapper.readValue(results.getObject("wildcard").toString(), IsaacWildcard.class));
    gameboardDO.setWildCardPosition(results.getInt("wildcard_position"));
    gameboardDO.setGameFilter(objectMapper
        .readValue(results.getObject("game_filter").toString(), GameFilter.class));
    long ownerUserId = results.getLong("owner_user_id");
    // by default getLong (primitive) sets null to 0, where 0 is a distinct value from null for user IDs
    gameboardDO.setOwnerUserId(!results.wasNull() ? ownerUserId : null);
    gameboardDO.setTags(objectMapper.readValue(results.getObject("tags").toString(), Set.class));
    if (results.getString("creation_method") != null) {
      gameboardDO.setCreationMethod(GameboardCreationMethod.valueOf(results.getString("creation_method")));
    }

    gameboardDO.setCreationDate(getInstantFromTimestamp(results, "creation_date"));
    return gameboardDO;
  }

  /**
   * Helper method to get a list of question ids from a dto.
   *
   * @param gameboardDTO to extract.
   * @return List of question ids for the gameboard provided.
   */
  private static List<GameboardContentDescriptor> getContentDescriptors(final GameboardDTO gameboardDTO) {
    List<GameboardContentDescriptor> listOfContentDescriptors = Lists.newArrayList();

    if (gameboardDTO.getContents() == null || gameboardDTO.getContents().isEmpty()) {
      return listOfContentDescriptors;
    }

    for (GameboardItem gameItem : gameboardDTO.getContents()) {
      if (gameItem.getId() == null || gameItem.getId().isEmpty()) {
        continue;
      }
      listOfContentDescriptors.add(new GameboardContentDescriptor(
          gameItem.getId(), gameItem.getContentType(), gameItem.getCreationContext()));
    }
    return listOfContentDescriptors;
  }

  // Field Constants
  // updateGameboardTitle
  private static final int FIELD_UPDATE_TITLE_TITLE = 1;
  private static final int FIELD_UPDATE_TITLE_ID = 2;

  // isBoardLinkedToUser
  private static final int FIELD_IS_BOARD_LINKED_TO_USER_USER_ID = 1;
  private static final int FIELD_IS_BOARD_LINKED_TO_USER_GAMEBOARD_ID = 2;

  // createOrUpdateUserLinkToGameboard
  private static final int FIELD_LINK_USER_USER_ID = 1;
  private static final int FIELD_LINK_USER_GAMEBOARD_ID = 2;
  private static final int FIELD_LINK_USER_CREATED = 3;
  private static final int FIELD_LINK_USER_LAST_VISITED = 4;

  // removeUserLinkToGameboard
  private static final int FIELD_REMOVE_USER_LINK_USER_ID = 1;
  private static final int FIELD_REMOVE_USER_LINK_GAMEBOARD_IDS_INITIAL_INDEX = 2;

  // getGameboardsByUserId
  private static final int FIELD_GET_GAMEBOARDS_BY_USER_USER_ID = 1;

  // saveGameboard
  private static final int FIELD_SAVE_GAMEBOARD_GAMEBOARD_ID = 1;
  private static final int FIELD_SAVE_GAMEBOARD_TITLE = 2;
  private static final int FIELD_SAVE_GAMEBOARD_CONTENTS = 3;
  private static final int FIELD_SAVE_GAMEBOARD_WILDCARD = 4;
  private static final int FIELD_SAVE_GAMEBOARD_WILDCARD_POSITION = 5;
  private static final int FIELD_SAVE_GAMEBOARD_GAME_FILTER = 6;
  private static final int FIELD_SAVE_GAMEBOARD_OWNER_USER_ID = 7;
  private static final int FIELD_SAVE_GAMEBOARD_CREATION_METHOD = 8;
  private static final int FIELD_SAVE_GAMEBOARD_TAGS = 9;
  private static final int FIELD_SAVE_GAMEBOARD_CREATION_DATE = 10;

  // getGameboardById
  private static final int FIELD_GET_BY_ID_GAMEBOARD_ID = 1;

  // getGameboardsByIds
  private static final int FIELD_GET_BY_IDS_GAMEBOARD_ID_LIST = 1;
}
