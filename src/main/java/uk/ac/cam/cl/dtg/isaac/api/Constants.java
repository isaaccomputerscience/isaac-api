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
package uk.ac.cam.cl.dtg.isaac.api;

/**
 * Utility class to provide common isaac-specific constants.
 *
 */
public final class Constants {
	public static final String MAILER_SMTP_SERVER = "MAILER_SMTP_SERVER";
	public static final String MAIL_FROM_ADDRESS = "MAIL_FROM_ADDRESS";
	public static final String MAIL_RECEIVERS = "MAIL_RECEIVERS";
	public static final String PROXY_PATH = "PROXY_PATH";
	public static final String ANALYTICS_TRACKING_ID = "ANALYTICS_TRACKING_ID";

	/*
	 * Subject specific constants.
	 */
	public static final String CONCEPT_TYPE = "isaacConceptPage";
	public static final String QUESTION_TYPE = "isaacQuestionPage";
	public static final String FAST_TRACK_QUESTION_TYPE = "isaacFastTrackQuestionPage";
	public static final String WILDCARD_TYPE = "isaacWildcard";
	public static final String PAGE_FRAGMENT_TYPE = "isaacPageFragment";
	public static final String PAGE_TYPE = "page";

	public static final String RELATED_CONTENT_FIELDNAME = "relatedContent";

	public static final String GAMEBOARD_COLLECTION_NAME = "gameboards";
	public static final String USERS_GAMEBOARD_COLLECTION_NAME = "UsersToGameboards";
	
	public static final String ASSIGNMENT_COLLECTION_NAME = "assignments";
	public static final String GROUP_ASSIGNMENT_COLLECTION_NAME = "groupToAssignments";
	
	/**
	 * Game specific variables.
	 */
	public static final int GAME_BOARD_TARGET_SIZE = 10;
	
	/**
	 * GameboardItemState
	 * Represents the potential states of a gameboard item.
	 */
	public enum GameboardItemState { COMPLETED, IN_PROGRESS, NOT_ATTEMPTED, TRY_AGAIN }
	
	/**
	 * GameboardState
	 * Represents the potential states of a gameboard.
	 */
	public enum GameboardState { COMPLETED, IN_PROGRESS, NOT_ATTEMPTED }
	// GameboardDTO field names
	public static final String CREATED_DATE_FIELDNAME = "created";
	public static final String VISITED_DATE_FIELDNAME = "lastVisited";	
	
	public static final Integer DEFAULT_GAMEBOARDS_RESULTS_LIMIT = 6;
	
	// Log events
	public static final String VIEW_QUESTION = "VIEW_QUESTION";
	public static final String QUESTION_ID_LOG_FIELDNAME = "questionId";
	public static final String ADD_BOARD_TO_PROFILE = "ADD_BOARD_TO_PROFILE";
	public static final String DELETE_BOARD_FROM_PROFILE = "ADD_BOARD_TO_PROFILE";
	public static final String GLOBAL_SITE_SEARCH = "GLOBAL_SITE_SEARCH";
	public static final String VIEW_CONCEPT = "VIEW_CONCEPT";
	public static final String CONCEPT_ID_LOG_FIELDNAME = "conceptId";
	public static final String VIEW_PAGE = "VIEW_PAGE";
	public static final String PAGE_ID_FIELDNAME = "CONCEPT_ID";
	public static final String PAGE_ID_LOG_FIELDNAME = "pageId";
	
	public static final String GENERATE_RANDOM_GAMEBOARD = "GENERATE_RANDOM_GAMEBOARD";
	public static final String GAMEBOARD_LOG_FIELDNAME = "GAMEBOARD_LOG_FIELDNAME";
	public static final String VIEW_MY_BOARDS_PAGE = "VIEW_MY_BOARDS_PAGE";
	
	/**
	 * Private constructor to prevent this class being created.
	 */
	private Constants() {
		// not allowed to create one of these as it wouldn't make sense.
	}
}
