package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * RoleNotAuthorisedException.
 * If the user is not of the correct role.
 * Created by sac92 on 30/04/2016.
 */
public class RoleNotAuthorisedException extends Exception {
  private static final long serialVersionUID = -284459943327239344L;

  /**
   * Exception constructor with message for when an action is attempted with a role that is not authorised to do so.
   *
   * @param message explaining the exception
   */
  public RoleNotAuthorisedException(final String message) {
    super(message);
  }

}
