/**
 * Copyright 2015 Alistair Stead
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

package uk.ac.cam.cl.dtg.segue.api.managers;

import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

/**
 * Interface to allow classes interested in group changes to register their interest.
 *
 * @author Alistair Stead
 */
public interface IGroupObserver {

  /**
   * Action to be taken when a user is removed from a group.
   *
   * @param group the group that the membership was revoked from
   * @param user  the user that revoked the membership
   */
  void onGroupMembershipRemoved(UserGroupDTO group, RegisteredUserDTO user);

  /**
   * Action to be taken when a user is added to a group.
   *
   * @param group the group that the user was added to
   * @param user  the user added to the group
   */
  void onMemberAddedToGroup(UserGroupDTO group, RegisteredUserDTO user);

  /**
   * Action to be taken when an additional manager is added to a group.
   *
   * @param group                 the group that the additional manager was added to
   * @param additionalManagerUser the additional manager added to the group
   */
  void onAdditionalManagerAddedToGroup(UserGroupDTO group, RegisteredUserDTO additionalManagerUser);

  /**
   * Action to be taken when an additional manager is promoted to group owner.
   *
   * @param group    the group for which the additional manager was promoted to owner
   * @param newOwner the additional manager that was promoted to group owner
   */
  void onAdditionalManagerPromotedToOwner(UserGroupDTO group, RegisteredUserDTO newOwner);

  /**
   * Action to be taken when additional manager privileges are changed for a group.
   *
   * @param group the group for which the additional manager permissions were changed
   */
  void onAdditionalManagerPrivilegesChanged(UserGroupDTO group);

}
