package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.databind.JsonNode;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;

/**
 * Created by du220 on 27/04/2018.
 */
public class UserBadge {

    private Long userId;
    private UserBadgeManager.Badge badgeName;
    private JsonNode state;

    /**
     * Constructor
     *
     * @param userId the id of the owner of the badge
     * @param badgeName the name of the badge
     * @param state an object specifying the current state of the badge
     */
    public UserBadge(final Long userId, final UserBadgeManager.Badge badgeName, final JsonNode state) {
        this.userId = userId;
        this.badgeName = badgeName;
        this.state = state;
    }

    /**
     * Gets the user id
     *
     * @return the user id
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * Gets the name of the badge (enum)
     *
     * @return the badge name
     */
    public UserBadgeManager.Badge getBadgeName() {
        return this.badgeName;
    }

    /**
     * Gets the current aggregate state of the badge
     *
     * @return an object specifying the current badge state
     */
    public JsonNode getState() {
        return this.state;
    }

    public void setState(final JsonNode state) {
        this.state = state;
    }
}
