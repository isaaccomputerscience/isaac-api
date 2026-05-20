package uk.ac.cam.cl.dtg.segue.api.managers;

public interface IExternalAccountManager {

  SyncResult synchroniseChangedUsers() throws ExternalAccountSynchronisationException;
}
