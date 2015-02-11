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
package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.Date;
import java.util.List;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupDataManager;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * GroupManager.
 * Responsible for managing group related logic.
 * @author sac92
 */
public class GroupManager {
	private final IUserGroupDataManager groupDatabase;
	private final UserManager userManager;
	private final MapperFacade dtoMapper;
	
	/**
	 * GroupManager.
	 * 
	 * @param groupDatabase
	 *            - the IUserGroupManager implementation
	 * @param userManager
	 *            - the user manager so that the group manager can get user details.
	 * @param dtoMapper
	 *            - Preconfigured dto mapper
	 */
	@Inject
	public GroupManager(final IUserGroupDataManager groupDatabase, final UserManager userManager,
			final MapperFacade dtoMapper) {
		Validate.notNull(groupDatabase);
		Validate.notNull(userManager);
		
		this.groupDatabase = groupDatabase;
		this.userManager = userManager;
		this.dtoMapper = dtoMapper;
	}

	/**
	 * createAssociationGroup.
	 * 
	 * @param groupName
	 *            - name describing the group.
	 * @param groupOwner
	 *            - the user who wishes to grant permissions to another.
	 * @return AssociationGroup
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public UserGroupDTO createUserGroup(final String groupName, final RegisteredUserDTO groupOwner)
		throws SegueDatabaseException {
		Validate.notBlank(groupName);
		Validate.notNull(groupOwner);

		UserGroup group = new UserGroup(null, groupName, groupOwner.getDbId(), new Date());

		return this.convertGroupToDTO(groupDatabase.createGroup(group));
	}

	/**
	 * createAssociationGroup.
	 * 
	 * @param groupToEdit
	 *            - group to edit.
	 * @return modified group.
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public UserGroupDTO editUserGroup(final UserGroup groupToEdit)
		throws SegueDatabaseException {
		Validate.notNull(groupToEdit);		
		
		return this.convertGroupToDTO(groupDatabase.editGroup(groupToEdit));
	}
	
	/**
	 * Delete Group.
	 * 
	 * @param group
	 *            - to delete
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void deleteGroup(final UserGroupDTO group) throws SegueDatabaseException {
		groupDatabase.deleteGroup(group.getId());		
		//TODO: assignments should probably be cleaned up
	}

	/**
	 * getUsersInGroup.
	 * @param group to find
	 * @return list of users who are members of the group
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public List<RegisteredUserDTO> getUsersInGroup(final UserGroupDTO group) throws SegueDatabaseException {		
		List<String> groupMemberIds = groupDatabase.getGroupMemberIds(group.getId());
		
		return userManager.findUsers(groupMemberIds);
	}

	/**
	 * getGroupsByOwner.
	 * 
	 * @param ownerUserId
	 *            - the owner of the group to search for.
	 * @return List of groups or empty list.
	 */
	public List<UserGroupDTO> getGroupsByOwner(final String ownerUserId) {
		return convertGroupToDTOs(groupDatabase.getGroupsByOwner(ownerUserId));
	}

	/**
	 * Adds a user to a group.
	 * 
	 * @param group - the group that the user should be added to
	 * @param userToAdd - the user to add to a group
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void addUserToGroup(final UserGroupDTO group, final RegisteredUserDTO userToAdd)
		throws SegueDatabaseException {
		groupDatabase.addUserToGroup(userToAdd.getDbId(), group.getId());
	}

	/**
	 * Removes a user from a group.
	 * 
	 * @param group - that should be affected
	 * @param userToRemove - user that should be removed.
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void removeUserFromGroup(final UserGroupDTO group, final RegisteredUserDTO userToRemove)
		throws SegueDatabaseException {
		groupDatabase.removeUserFromGroup(userToRemove.getDbId(), group.getId());
	}

	/**
	 * Find by Id.
	 * 
	 * @param groupId
	 *            to search for.
	 * @return group or null.
	 */
	public UserGroupDTO getGroupById(final String groupId) {
		return convertGroupToDTO(groupDatabase.findById(groupId));
	}

	/**
	 * Determine if a group id exists and is valid.
	 * 
	 * @param groupId
	 *            - group id
	 * @return true if it does false if not.
	 */
	public boolean isValidGroup(final String groupId) {
		return this.groupDatabase.findById(groupId) != null;
	}
	
	/**
	 * @param group to convert
	 * @return groupDTO
	 */
	private UserGroupDTO convertGroupToDTO(final UserGroup group) {
		return dtoMapper.map(group, UserGroupDTO.class);
	}
	
	/**
	 * @param groups to convert
	 * @return groupDTOs
	 */
	private List<UserGroupDTO> convertGroupToDTOs(final List<UserGroup> groups) {
		List<UserGroupDTO> result = Lists.newArrayList();
		for (UserGroup group : groups) {
			result.add(convertGroupToDTO(group));
		}
		return result;
	}
}
